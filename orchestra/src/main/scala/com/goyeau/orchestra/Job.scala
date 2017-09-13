package com.goyeau.orchestra

import java.io.{FileOutputStream, _}
import java.nio.file.Files
import java.time.Instant
import java.util.UUID

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.java8.time._
import com.goyeau.orchestra.ARunStatus._
import com.goyeau.orchestra.kubernetes.{JobUtils, PodConfig}
import shapeless._
import shapeless.ops.function.FnToProduct
import com.goyeau.orchestra.ARunStatus._

object Job {

  def apply[ParamValues <: HList] = new DefinitionBuilder[ParamValues]

  class DefinitionBuilder[ParamValues <: HList] {
    def apply[JobFn](id: Symbol)(
      implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Unit],
      encoder: Encoder[ParamValues],
      decoder: Decoder[ParamValues]
    ) = Definition[ParamValues, JobFn](id)
  }

  case class Definition[ParamValues <: HList: Encoder: Decoder, JobFn](id: Symbol)(
    implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Unit]
  ) {

    def apply(job: JobFn) = Runner(this, PodConfig(HNil), fnToProd(job))

    def apply[Containers <: HList](podConfig: PodConfig[Containers]) = new RunnerBuilder[Containers](podConfig)

    class RunnerBuilder[Containers <: HList](podConfig: PodConfig[Containers]) {
      def apply[PodJobFn](job: PodJobFn)(
        implicit podFnToProd: FnToProduct.Aux[PodJobFn, Containers => JobFn]
      ) = Runner[ParamValues, Containers](
        Definition.this,
        podConfig,
        fnToProd(podFnToProd(job)(podConfig.containers))
      )
    }

    trait Api {
      def trigger(runInfo: RunInfo, params: ParamValues): ARunStatus
      def logs(runId: UUID, from: Int): Seq[(Option[Symbol], String)]
      def runs(): Seq[(UUID, Instant, ARunStatus)]
    }

    object Api {
      def router(apiServer: Api)(implicit ec: ExecutionContext) = AutowireServer.route[Api](apiServer)

      val client = AutowireClient(id)[Api]
    }
  }

  case class Runner[ParamValues <: HList: Encoder: Decoder, Containers <: HList](
    definition: Definition[ParamValues, _],
    podConfig: PodConfig[Containers],
    job: ParamValues => Unit
  ) {

    private val logDelimiter = "_OrchestraDelimiter_"

    def start(runInfo: RunInfo): Unit =
      if (OrchestraConfig.paramsFilePath(runInfo).toFile.exists()) {
        OrchestraConfig.logsDirPath(runInfo.runId).toFile.mkdirs()
        val logsOut =
          new LogsPrintStream(new FileOutputStream(OrchestraConfig.logsFilePath(runInfo.runId).toFile, true),
                              true,
                              logDelimiter,
                              None)

        Utils.withOutErr(logsOut) {
          try {
            RunStatusUtils.save(runInfo, ARunStatus.Running(Instant.now()))

            job(
              AutowireServer.read[ParamValues](
                Source.fromFile(OrchestraConfig.paramsFilePath(runInfo).toFile).mkString
              )
            )
            println("Job completed")

            RunStatusUtils.save(runInfo, ARunStatus.Success(Instant.now()))
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              RunStatusUtils.save(runInfo, ARunStatus.Failure(Instant.now(), e))
          } finally {
            logsOut.close()
            JobUtils.delete(runInfo)
          }
        }
      }

    val apiServer = new definition.Api {
      override def trigger(runInfo: RunInfo, params: ParamValues): ARunStatus =
        if (OrchestraConfig.statusFilePath(runInfo).toFile.exists()) RunStatusUtils.current(runInfo)
        else {
          OrchestraConfig.runDirPath(runInfo).toFile.mkdirs()

          val triggered = RunStatusUtils.save(runInfo, ARunStatus.Triggered(Instant.now()))
          Files.write(OrchestraConfig.paramsFilePath(runInfo), AutowireServer.write(params).getBytes)

          Await.result(JobUtils.create(runInfo, podConfig), Duration.Inf)
          triggered
        }

      // @TODO Move that in a common api
      override def logs(runId: UUID, from: Int): Seq[(Option[Symbol], String)] = {
        val stageRegex = s"(.+)$logDelimiter(.+)".r
        Seq(OrchestraConfig.logsFilePath(runId).toFile)
          .filter(_.exists())
          .flatMap(
            Source
              .fromFile(_)
              .getLines()
              .slice(from, Int.MaxValue)
              .map {
                case stageRegex(line, stage) => (Option(Symbol(stage)), line)
                case line                    => (None, line)
              }
              .toSeq
          )
      }

      override def runs(): Seq[(UUID, Instant, ARunStatus)] =
        Seq(OrchestraConfig.jobDirPath(definition.id).toFile)
          .filter(_.exists())
          .flatMap(_.listFiles())
          .map(runDir => RunInfo(definition.id, Option(UUID.fromString(runDir.getName))))
          .filter(OrchestraConfig.statusFilePath(_).toFile.exists())
          .flatMap { runInfo =>
            RunStatusUtils.history(runInfo).headOption.map {
              case status: Triggered => (runInfo.runId, status.at, RunStatusUtils.current(runInfo))
              case status =>
                throw new IllegalStateException(s"$status is not of status type ${classOf[Triggered].getName}")
            }
          }
          .sortBy(_._2)
          .reverse
    }

    def apiRoute(implicit ec: ExecutionContext): Route =
      path(definition.id.name / Segments) { segments =>
        post {
          entity(as[String]) { entity =>
            val body = AutowireServer.read[Map[String, String]](entity)
            val request = definition.Api.router(apiServer).apply(Core.Request(segments, body))
            onSuccess(request)(complete(_))
          }
        }
      }
  }
}

class LogsPrintStream(out: OutputStream, autoFlush: Boolean, delimiter: String, stageId: Option[Symbol])
    extends PrintStream(out, autoFlush) {

  private val stageInfo = stageId.map(stageId => s"$delimiter${stageId.name}\n")
  private def insertStageInfo(s: String) =
    stageInfo.fold(s)(added => s.replaceAll("\r", "").replaceAll("\n", s"$added\n"))

  override def print(s: String): Unit = super.print(insertStageInfo(s))
  override def println(s: String): Unit = super.println(insertStageInfo(s) + stageInfo.mkString)
}
