package com.goyeau.orchestra

import java.io.{FileOutputStream, _}
import java.nio.file.Files
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import com.goyeau.orchestra.ARunStatus._
import com.goyeau.orchestra.kubernetes.{JobUtils, PodConfig}
import shapeless._
import shapeless.ops.function.FnToProduct
import com.goyeau.orchestra.ARunStatus._
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax

object Job {

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

  case class Definition[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](id: Symbol) {

    def apply(job: JobFn)(implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result]) =
      Runner(this, PodConfig(), fnToProd(job))

    def apply[Containers <: HList](podConfig: PodConfig[Containers]) = new RunnerBuilder[Containers](podConfig)

    class RunnerBuilder[Containers <: HList](podConfig: PodConfig[Containers]) {
      def apply[PodJobFn](job: PodJobFn)(
        implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
        podFnToProd: FnToProduct.Aux[PodJobFn, Containers => JobFn]
      ) = Runner[ParamValues, Result, Containers](
        Definition.this,
        podConfig,
        fnToProd(podFnToProd(job)(podConfig.containers))
      )
    }

    trait Api {
      def trigger(runInfo: RunInfo, params: ParamValues, tags: Seq[String] = Seq.empty): ARunStatus
      def tags(): Seq[String]
      def runs(page: Page[Instant]): Seq[(UUID, Instant, ARunStatus)]
    }

    object Api {
      def router(apiServer: Api)(implicit ec: ExecutionContext) = AutowireServer.route[Api](apiServer)

      val client = new autowire.Client[String, Decoder, Encoder] {
        import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

        override def doCall(req: Request): Future[String] =
          Ajax
            .post(
              url = (Jobs.apiSegment +: Jobs.jobSegment +: id.name +: req.path).mkString("/"),
              data = req.args.asJson.noSpaces,
              responseType = "application/json",
              headers = Map("Content-Type" -> "application/json")
            )
            .map(_.responseText)

        override def read[T: Decoder](raw: String) = decode[T](raw).fold(throw _, identity)
        override def write[T: Encoder](obj: T) = obj.asJson.noSpaces
      }.apply[Api]
    }
  }

  case class Runner[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder, Containers <: HList](
    definition: Definition[_, ParamValues, Result],
    podConfig: PodConfig[Containers],
    job: ParamValues => Result
  ) {

    def run(runInfo: RunInfo): Unit = {
      RunStatusUtils.runInit(runInfo, Seq.empty)
      val logsOut = LoggingHelpers(new FileOutputStream(OrchestraConfig.logsFile(runInfo.runId).toFile, true))

      Utils.withOutErr(logsOut) {
        val running = RunStatusUtils.notifyRunning(runInfo)

        try {
          val paramFile = OrchestraConfig.paramsFile(runInfo).toFile
          job(
            AutowireServer.read[ParamValues](
              if (paramFile.exists()) Source.fromFile(paramFile).mkString
              else "[]"
            )
          )
          println("Job completed")

          running.succeed(runInfo)
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            running.fail(runInfo, e)
        } finally {
          logsOut.close()
          JobUtils.selfDelete(runInfo)
        }
      }
    }

    val apiServer = new definition.Api {
      override def trigger(runInfo: RunInfo, params: ParamValues, tags: Seq[String] = Seq.empty): ARunStatus =
        if (OrchestraConfig.statusFile(runInfo).toFile.exists()) RunStatusUtils.current(runInfo)
        else {
          RunStatusUtils.runInit(runInfo, tags)

          val triggered = RunStatusUtils.notifyTriggered(runInfo)
          Files.write(OrchestraConfig.paramsFile(runInfo), AutowireServer.write(params).getBytes)

          Await.result(JobUtils.create(runInfo, podConfig), Duration.Inf)
          triggered
        }

      override def tags(): Seq[String] = OrchestraConfig.tagsDir(definition.id).toFile.list()

      override def runs(page: Page[Instant]): Seq[(UUID, Instant, ARunStatus)] = {
        val from = page.from.fold(LocalDateTime.MAX)(LocalDateTime.ofInstant(_, ZoneOffset.UTC))

        val runs = for {
          runsByDate <- Stream(OrchestraConfig.runsDirByDate(definition.id).toFile)
          if runsByDate.exists()
          yearDir <- runsByDate.listFiles().toStream.sortBy(-_.getName.toInt).dropWhile(_.getName.toInt > from.getYear)
          dayDir <- yearDir
            .listFiles()
            .toStream
            .sortBy(-_.getName.toInt)
            .dropWhile(_.getName.toInt > from.getDayOfYear)
          secondDir <- dayDir
            .listFiles()
            .toStream
            .sortBy(-_.getName.toInt)
            .dropWhile(_.getName.toInt > from.toEpochSecond(ZoneOffset.UTC))
          runId <- secondDir.list().toStream

          runInfo = RunInfo(definition.id, Option(UUID.fromString(runId)))
          if OrchestraConfig.statusFile(runInfo).toFile.exists()
          at <- RunStatusUtils.history(runInfo).headOption.map {
            case status: Triggered => status.at
            case status: Running   => status.at
            case status =>
              throw new IllegalStateException(s"$status is not of status type ${classOf[Triggered].getName}")
          }
        } yield (runInfo.runId, at, RunStatusUtils.current(runInfo))

        runs.take(page.size)
      }
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
