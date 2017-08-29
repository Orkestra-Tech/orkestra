package com.goyeau.orchestra

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.nio.file.attribute.BasicFileAttributes
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
      def run(runInfo: RunInfo, params: ParamValues): ARunStatus[Result]
      def logs(runId: UUID, from: Int): Seq[String]
      def runs(): Seq[(UUID, Instant)]
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

    def run(runInfo: RunInfo): Unit = {
      new File(OrchestraConfig.runDirPath(runInfo)).mkdirs()
      val logsOut = new PrintStream(new FileOutputStream(OrchestraConfig.logsFilePath(runInfo), true), true)

      Utils.withOutErr(logsOut) {
        val statusPath = OrchestraConfig.statusFilePath(runInfo)
        try {
          Files.write(
            Paths.get(statusPath),
            s"${AutowireServer.write[ARunStatus[Result]](ARunStatus.Running(Instant.now()))}\n".getBytes,
            StandardOpenOption.APPEND
          )
          val result =
            job(AutowireServer.read[ParamValues](Source.fromFile(OrchestraConfig.paramsFilePath(runInfo)).mkString))
          println("Job completed")
          Files.write(
            Paths.get(statusPath),
            s"${AutowireServer.write[ARunStatus[Result]](ARunStatus.Success(Instant.now(), result))}\n".getBytes,
            StandardOpenOption.APPEND
          )
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            Files.write(
              Paths.get(statusPath),
              s"${AutowireServer.write[ARunStatus[Result]](ARunStatus.Failure(e))}\n".getBytes,
              StandardOpenOption.APPEND
            )
        } finally {
          logsOut.close()
          JobUtils.delete(runInfo)
        }
      }
    }

    val apiServer = new definition.Api {
      override def run(runInfo: RunInfo, params: ParamValues): ARunStatus[Result] = {
        val runDir = new File(OrchestraConfig.runDirPath(runInfo))
        val statusFile = new File(OrchestraConfig.statusFilePath(runInfo))

        if (runDir.exists() && statusFile.exists()) RunStatusUtils.load[Result](runInfo).last
        else if (runDir.exists() && !statusFile.exists()) ARunStatus.Unknown
        else {
          runDir.mkdirs()
          Files.write(Paths.get(OrchestraConfig.paramsFilePath(runInfo)), AutowireServer.write(params).getBytes)

          JobUtils.create(runInfo, podConfig)

          val status = ARunStatus.Scheduled(Instant.now())
          Files.write(statusFile.toPath, s"${AutowireServer.write[ARunStatus[Result]](status)}\n".getBytes)
          status
        }
      }

      override def logs(runId: UUID, from: Int): Seq[String] =
        Seq(new File(OrchestraConfig.logsFilePath(RunInfo(definition.id, Some(runId)))))
          .filter(_.exists())
          .flatMap(Source.fromFile(_).getLines().slice(from, Int.MaxValue).toSeq)

      override def runs(): Seq[(UUID, Instant)] =
        Seq(new File(OrchestraConfig.jobDirPath(definition.id)))
          .filter(_.exists())
          .flatMap(_.listFiles())
          .filter(_.isDirectory())
          .flatMap { runDir =>
            val runInfo = RunInfo(definition.id, Option(UUID.fromString(runDir.getName)))
            RunStatusUtils.load[Result](runInfo).headOption.map {
              case status: Scheduled => (runInfo.runId, status.at)
              case status =>
                throw new IllegalStateException(s"$status is not of status type ${classOf[Scheduled].getName}")
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
