package com.goyeau.orchestra

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}
import java.time.Instant
import java.util.UUID

import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import autowire.Core
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import com.goyeau.orchestra.ARunStatus._
import com.goyeau.orchestra.kubernetes.{JobScheduler, PodConfig}
import shapeless._
import shapeless.ops.function.FnToProduct

object Job {
  case class Definition[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](id: Symbol) {

    def apply(job: JobFn)(implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result]) =
      Runner(this, PodConfig(HNil), fnToProd(job))

    def apply(magnet: ParamMagnet): magnet.Out = magnet(this)

    // Trick to hide shapeless types and implicits
    sealed trait ParamMagnet {
      type Out
      def apply(definition: Definition[JobFn, ParamValues, Result]): Out
    }

    object ParamMagnet {

      implicit def apply[Containers <: HList, PodJobFn](podConfig: PodConfig[Containers])(
        implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
        podFnToProd: FnToProduct.Aux[PodJobFn, Containers => JobFn]
      ) =
        new ParamMagnet {
          type Out = PodJobFn => Runner[JobFn, ParamValues, Result, Containers]
          def apply(definition: Definition[JobFn, ParamValues, Result]) =
            (job: PodJobFn) =>
              Runner[JobFn, ParamValues, Result, Containers](
                definition,
                podConfig,
                fnToProd(podFnToProd(job)(podConfig.containers))
            )
        }
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

    def run(runInfo: RunInfo): Unit = {
      val runPath = s"${OrchestraConfig.home}/${definition.id.name}/${runInfo.runId}"
      new File(runPath).mkdirs()

      val logsOut = new PrintStream(new FileOutputStream(s"$runPath/logs", true), true)
      val statusWriter = new PrintWriter(s"$runPath/status")

      try {
        Utils.withOutErr(logsOut) {
          statusWriter.append(
            AutowireServer.write[ARunStatus[Result]](ARunStatus.Running(Instant.now().toEpochMilli)) + "\n"
          )
          val result = job(AutowireServer.read[ParamValues](Source.fromFile(s"$runPath/params").mkString))
          statusWriter.append(
            AutowireServer.write[ARunStatus[Result]](ARunStatus.Success(Instant.now().toEpochMilli, result)) + "\n"
          )
        }
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          statusWriter.append(AutowireServer.write(ARunStatus.Failed(e)))
      } finally {
        statusWriter.close()
        logsOut.close()
      }
    }

    def apiServer(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) = new definition.Api {
      override def run(runInfo: RunInfo, params: ParamValues): ARunStatus[Result] = {
        val runPath = s"${OrchestraConfig.home}/${definition.id.name}/${runInfo.runId}"
        val runDir = new File(runPath)
        val statusFile = new File(s"$runPath/status")

        if (runDir.exists() && statusFile.exists())
          AutowireServer.read[ARunStatus[Result]](Source.fromFile(statusFile).getLines().toSeq.last)
        else if (runDir.exists() && !statusFile.exists()) ARunStatus.Unknown
        else {
          runDir.mkdirs()
          val paramsWriter = new PrintWriter(s"$runPath/params")
          try paramsWriter.write(AutowireServer.write(params))
          finally paramsWriter.close()

          val status = Await
            .ready(JobScheduler(runInfo, podConfig), 1.minute)
            .value
            .get
            .fold[ARunStatus[Result]](e => ARunStatus.Failed(e), _ => ARunStatus.Scheduled(Instant.now().toEpochMilli))

          val statusWriter = new PrintWriter(statusFile)
          statusWriter.append(AutowireServer.write(status) + "\n")
          statusWriter.close()
          status
        }
      }

      override def logs(runId: UUID, from: Int): Seq[String] = {
        val runPath = s"${OrchestraConfig.home}/${definition.id.name}/$runId"
        Option(new File(s"$runPath/logs")).filter(_.exists).fold(Seq.empty[String]) { a =>
          Source.fromFile(a).getLines().slice(from, Int.MaxValue).toSeq
        }
      }

      def runs(): Seq[UUID] =
        Seq(new File(s"${OrchestraConfig.home}/${definition.id.name}"))
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

  def apply[JobFn](magnet: ParamMagnet[JobFn]): magnet.Out = magnet()

  // Trick to hide shapeless types and implicits
  sealed trait ParamMagnet[JobFn] {
    type Out
    def apply(): Out
  }

  object ParamMagnet {

    implicit def apply[JobFn, ParamValues <: HList, Result](id: Symbol)(
      implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) =
      new ParamMagnet[JobFn] {
        type Out = Definition[JobFn, ParamValues, Result]
        def apply() = Definition[JobFn, ParamValues, Result](id)
      }
  }
}
