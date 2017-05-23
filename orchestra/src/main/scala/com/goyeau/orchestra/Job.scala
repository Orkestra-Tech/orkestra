package com.goyeau.orchestra

import java.io.{File, FileOutputStream, PrintStream, PrintWriter}
import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import autowire.Core
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.generic.auto._
import shapeless.HList
import shapeless.ops.function.FnToProduct

object Job {
  case class Definition[Func, ParamValues <: HList: Encoder: Decoder](id: Symbol) {

    def apply[Result](func: Func)(implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result]) =
      Runner(this, fnToProd(func))

    trait Api {
      def run(runInfo: RunInfo, params: ParamValues): ARunStatus
      def logs(runId: UUID): String
    }

    object Api {
      def router(apiServer: Api)(implicit ec: ExecutionContext) =
        AutowireServer.route[Api](apiServer)

      val client = AutowireClient(id)[Api]
    }
  }

  case class Runner[Func, ParamValues <: HList: Encoder: Decoder, Result](definition: Definition[Func, ParamValues],
                                                                          job: ParamValues => Result) {

    def run(runInfo: RunInfo): Unit = {
      val runPath = s"${OrchestraConfig.home}/${definition.id.name}/${runInfo.id}"
      new File(runPath).mkdirs()

      val logsOut = new PrintStream(new FileOutputStream(s"$runPath/logs"), true)
      val statusWriter = new PrintWriter(s"$runPath/status")
      val runningStatus = ARunStatus.Running(Instant.now().toEpochMilli)

      Utils.withOutErr(logsOut) {
        try {
          statusWriter.write(AutowireServer.write(runningStatus))
          job(AutowireServer.read[ParamValues](Source.fromFile(s"$runPath/params").mkString))
          statusWriter.write(AutowireServer.write(ARunStatus.Success))
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            statusWriter.write(AutowireServer.write(ARunStatus.Failed(e)))
        } finally statusWriter.close()
      }
    }

    def apiServer(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) = new definition.Api {
      override def run(runInfo: RunInfo, params: ParamValues): ARunStatus = {
        val runPath = s"${OrchestraConfig.home}/${definition.id.name}/${runInfo.id}"
        new File(runPath).mkdirs()

        val runningStatus = ARunStatus.Running(Instant.now().toEpochMilli) // TODO: Should be Scheduling
        val paramsWriter = new PrintWriter(s"$runPath/params")
        try paramsWriter.write(AutowireServer.write(params))
        finally paramsWriter.close()

        kubernetes.Job.schedule(definition.id, runInfo)

        runningStatus
      }

      override def logs(runId: UUID): String = {
        val runPath = s"${OrchestraConfig.home}/${definition.id.name}/$runId"
        Option(new File(s"$runPath/logs")).filter(_.exists).fold("")(Source.fromFile(_).mkString)
      }
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

  def apply[Func](magnet: ParamMagnet[Func]): magnet.Out = magnet()

  // Trick to hide shapeless types and implicits
  sealed trait ParamMagnet[Func] {
    type Out
    def apply(): Out
  }

  object ParamMagnet {

    implicit def apply[Func, ParamValues <: HList, Result](id: Symbol)(
      implicit fnToProd: FnToProduct.Aux[Func, ParamValues => Result],
      encoder: Encoder[ParamValues],
      decoder: Decoder[ParamValues]
    ) =
      new ParamMagnet[Func] {
        type Out = Definition[Func, ParamValues]
        def apply() = Definition[Func, ParamValues](id)
      }
  }
}
