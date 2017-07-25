package com.goyeau.orchestra

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}
import java.time.Instant
import java.util.UUID

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import autowire.Core
import _root_.io.circe.{Decoder, Encoder}
import _root_.io.circe.generic.auto._
import com.goyeau.orchestra.ARunStatus._
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
      def runs(): Seq[UUID]
    }

    object Api {
      def router(apiServer: Api)(implicit ec: ExecutionContext) =
        AutowireServer.route[Api](apiServer)

      val client = AutowireClient(id)[Api]
    }
  }

  case class Runner[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder, Containers <: HList](
    definition: Definition[JobFn, ParamValues, Result],
    podConfig: PodConfig[Containers],
    job: ParamValues => Result
  ) {

    def run(runInfo: RunInfo)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Unit = {
      new File(Config.runDirPath(runInfo)).mkdirs()
      val logsOut = new PrintStream(new FileOutputStream(Config.logsFilePath(runInfo), true), true)
      val statusWriter = new PrintWriter(Config.statusFilePath(runInfo))

      Utils.withOutErr(logsOut) {
        try {
          statusWriter.append(
            AutowireServer.write[ARunStatus[Result]](ARunStatus.Running(Instant.now().toEpochMilli)) + "\n"
          )
          val result =
            job(AutowireServer.read[ParamValues](Source.fromFile(Config.paramsFilePath(runInfo)).mkString))
          statusWriter.append(
            AutowireServer.write[ARunStatus[Result]](ARunStatus.Success(Instant.now().toEpochMilli, result)) + "\n"
          )
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            statusWriter.append(AutowireServer.write(ARunStatus.Failure(e)))
        } finally {
          statusWriter.close()
          logsOut.close()
          kubernetes.Job.delete(runInfo)
        }
      }
    }

    def apiServer(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) = new definition.Api {
      override def run(runInfo: RunInfo, params: ParamValues): ARunStatus[Result] = {
        val runDir = new File(Config.runDirPath(runInfo))
        val statusFile = new File(Config.statusFilePath(runInfo))

        if (runDir.exists() && statusFile.exists())
          AutowireServer.read[ARunStatus[Result]](Source.fromFile(statusFile).getLines().toSeq.last)
        else if (runDir.exists() && !statusFile.exists()) ARunStatus.Unknown
        else {
          runDir.mkdirs()
          val paramsWriter = new PrintWriter(Config.paramsFilePath(runInfo))
          try paramsWriter.write(AutowireServer.write(params))
          finally paramsWriter.close()

          kubernetes.Job.create(runInfo, podConfig)

          val status = ARunStatus.Scheduled(Instant.now().toEpochMilli)
          val statusWriter = new PrintWriter(statusFile)
          try statusWriter.append(AutowireServer.write(status) + "\n")
          finally statusWriter.close()
          status
        }
      }

      override def logs(runId: UUID, from: Int): Seq[String] =
        Option(new File(Config.logsFilePath(RunInfo(definition.id, Some(runId)))))
          .filter(_.exists)
          .fold(Seq.empty[String]) { a =>
            Source.fromFile(a).getLines().slice(from, Int.MaxValue).toSeq
          }

      def runs(): Seq[UUID] =
        Seq(new File(Config.jobDirPath(definition.id)))
          .filter(_.exists())
          .flatMap(_.listFiles())
          .filter(_.isDirectory)
          .sortBy(_.lastModified)
          .reverse
          .map(runDir => UUID.fromString(runDir.getName))
    }

    def apiRoute(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Route =
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
