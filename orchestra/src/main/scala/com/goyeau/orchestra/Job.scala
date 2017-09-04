package com.goyeau.orchestra

import java.io.{File, FileOutputStream, PrintStream}
import java.nio.file.{Files, Paths}
import java.time.Instant
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.java8.time._
import ARunStatus._
import com.goyeau.orchestra.kubernetes.{JobUtils, PodConfig}
import shapeless._
import shapeless.ops.function.FnToProduct

import com.goyeau.orchestra.ARunStatus._

object Job {
  case class Definition[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](id: Symbol) {

    def apply(job: JobFn)(implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result]) =
      Runner(this, PodConfig(HNil), fnToProd(job))

    def apply[Containers <: HList](podConfig: PodConfig[Containers]) = new RunnerBuilder[Containers](podConfig)

    class RunnerBuilder[Containers <: HList](podConfig: PodConfig[Containers]) {
      def apply[PodJobFn](job: PodJobFn)(
        implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
        podFnToProd: FnToProduct.Aux[PodJobFn, Containers => JobFn]
      ) = Runner[JobFn, ParamValues, Result, Containers](
        Definition.this,
        podConfig,
        fnToProd(podFnToProd(job)(podConfig.containers))
      )
    }

    trait Api {
      def trigger(runInfo: RunInfo, params: ParamValues): ARunStatus
      def logs(runId: UUID, from: Int): Seq[String]
      def runs(): Seq[(UUID, Instant, ARunStatus)]
    }

    object Api {
      def router(apiServer: Api)(implicit ec: ExecutionContext) = AutowireServer.route[Api](apiServer)

      val client = AutowireClient(id)[Api]
    }
  }

  case class Runner[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder, Containers <: HList](
    definition: Definition[JobFn, ParamValues, Result],
    podConfig: PodConfig[Containers],
    job: ParamValues => Result
  ) {

    def run(runInfo: RunInfo): Unit =
      if (OrchestraConfig.paramsFilePath(runInfo).toFile.exists()) {
        val logsOut = new PrintStream(new FileOutputStream(OrchestraConfig.logsFilePath(runInfo).toFile, true), true)

        Utils.withOutErr(logsOut) {
          try {
            RunStatusUtils.save(runInfo, ARunStatus.Running(Instant.now()))

            job(
              AutowireServer.read[ParamValues](Source.fromFile(OrchestraConfig.paramsFilePath(runInfo).toFile).mkString)
            )
            println("Job completed")

            RunStatusUtils.save(runInfo, ARunStatus.Success(Instant.now()))
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              RunStatusUtils.save(runInfo, ARunStatus.Failure(e))
          } finally {
            logsOut.close()
            JobUtils.delete(runInfo)
          }
        }
      }

    val apiServer = new definition.Api {
      private val triggerLock = new Object

      override def trigger(runInfo: RunInfo, params: ParamValues): ARunStatus = triggerLock.synchronized {
        if (OrchestraConfig.statusFilePath(runInfo).toFile.exists()) RunStatusUtils.current(runInfo)
        else {
          OrchestraConfig.runDirPath(runInfo).toFile.mkdirs()

          val triggered = RunStatusUtils.save(runInfo, ARunStatus.Triggered(Instant.now()))
          Files.write(OrchestraConfig.paramsFilePath(runInfo), AutowireServer.write(params).getBytes)

          JobUtils.create(runInfo, podConfig)
          triggered
        }
      }

      override def logs(runId: UUID, from: Int): Seq[String] =
        Seq(OrchestraConfig.logsFilePath(RunInfo(definition.id, Some(runId))).toFile)
          .filter(_.exists())
          .flatMap(Source.fromFile(_).getLines().slice(from, Int.MaxValue).toSeq)

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

  def apply[JobFn] = new DefinitionBuilder[JobFn]

  class DefinitionBuilder[JobFn] {
    def apply[ParamValues <: HList, Result](id: Symbol)(
      implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = Definition[JobFn, ParamValues, Result](id)
  }
}
