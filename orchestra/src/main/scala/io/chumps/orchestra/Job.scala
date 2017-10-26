package io.chumps.orchestra

import java.io.FileOutputStream
import java.nio.file.Files
import java.time.{Instant, LocalDateTime, ZoneOffset}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._

import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.BaseEncoders._
import io.chumps.orchestra.kubernetes.JobUtils
import shapeless._
import shapeless.ops.function.FnToProduct

import io.circe.parser._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import io.k8s.api.core.v1.PodSpec
import org.scalajs.dom.ext.Ajax

import io.chumps.orchestra.model._

object Job {

  def apply[JobFn] = new DefinitionBuilder[JobFn]

  class DefinitionBuilder[JobFn] {
    def apply[ParamValues <: HList, Result](id: Symbol, name: String)(
      implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result],
      encoderP: Encoder[ParamValues],
      decoderP: Decoder[ParamValues],
      encoderR: Encoder[Result],
      decoderR: Decoder[Result]
    ) = Definition[JobFn, ParamValues, Result](id, name)
  }

  case class Definition[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](id: Symbol,
                                                                                                 name: String) {

    def apply(job: JobFn)(implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result]) =
      Runner(this, PodSpec(Seq.empty), fnToProd(job))

    def apply(podConfig: PodSpec)(job: JobFn)(implicit fnToProd: FnToProduct.Aux[JobFn, ParamValues => Result]) =
      Runner[ParamValues, Result](Definition.this, podConfig, fnToProd(job))

    private[orchestra] trait Api {
      def trigger(runId: RunId, params: ParamValues, tags: Seq[String] = Seq.empty): ARunStatus
      def stop(runId: RunId): Unit
      def tags(): Seq[String]
      def history(page: Page[Instant]): Seq[(RunId, Instant, ParamValues, Seq[String], ARunStatus, Seq[AStageStatus])]
    }

    private[orchestra] object Api {
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

  object Definition {

    implicit def encoder[JobFn, ParamValues <: HList, Result]: Encoder[Definition[JobFn, ParamValues, Result]] =
      (o: Job.Definition[JobFn, ParamValues, Result]) =>
        Json.obj(
          "id" -> o.id.asJson,
          "name" -> Json.fromString(o.name)
      )

    implicit def decoder[JobFn, ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder]
      : Decoder[Definition[JobFn, ParamValues, Result]] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[Symbol]
          name <- c.downField("name").as[String]
        } yield Job.Definition[JobFn, ParamValues, Result](id, name)

    implicit val decoderDefault: Decoder[Definition[_, _ <: HList, _]] = (c: HCursor) =>
      for {
        id <- c.downField("id").as[Symbol]
        name <- c.downField("name").as[String]
      } yield Job.Definition[Nothing, HNil, Nothing](id, name)
  }

  case class Runner[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](
    definition: Definition[_, ParamValues, Result],
    podSpec: PodSpec,
    job: ParamValues => Result
  ) {

    private[orchestra] def run(runInfo: RunInfo): Unit = {
      Utils.runInit(runInfo, Seq.empty)
      val logsOut = StagesHelpers(new FileOutputStream(OrchestraConfig.logsFile(runInfo.runId).toFile, true))

      Utils.withOutErr(logsOut) {
        val running = ARunStatus.persist(runInfo, ARunStatus.Running(Instant.now()))

        try {
          val paramFile = OrchestraConfig.paramsFile(runInfo).toFile
          job(
            if (paramFile.exists()) decode[ParamValues](Source.fromFile(paramFile).mkString).fold(throw _, identity)
            else HNil.asInstanceOf[ParamValues]
          )
          println("Job completed")

          running.succeed(runInfo)
        } catch {
          case e: Throwable =>
            e.printStackTrace()
            running.fail(runInfo, e)
        } finally {
          logsOut.close()
          JobUtils.selfDelete()
        }
      }
    }

    private[orchestra] object ApiServer extends definition.Api {
      override def trigger(runId: RunId, params: ParamValues, tags: Seq[String] = Seq.empty): ARunStatus = {
        val runInfo = RunInfo(definition, runId)
        if (OrchestraConfig.statusFile(runInfo).toFile.exists()) ARunStatus.current(runInfo)
        else {
          Utils.runInit(runInfo, tags)

          val triggered = ARunStatus.persist(runInfo, ARunStatus.Triggered(Instant.now()))
          Files.write(OrchestraConfig.paramsFile(runInfo), AutowireServer.write(params).getBytes)

          Await.result(JobUtils.create(runInfo, podSpec), Duration.Inf)
          triggered
        }
      }

      override def stop(runId: RunId): Unit = JobUtils.delete(RunInfo(definition, runId))

      override def tags(): Seq[String] = OrchestraConfig.tagsDir(definition.id).toFile.list()

      override def history(
        page: Page[Instant]
      ): Seq[(RunId, Instant, ParamValues, Seq[String], ARunStatus, Seq[AStageStatus])] = {
        val from = page.from.fold(LocalDateTime.MAX)(LocalDateTime.ofInstant(_, ZoneOffset.UTC))

        val runs = for {
          runsByDate <- Stream(OrchestraConfig.runsByDateDir(definition.id).toFile)
          if runsByDate.exists()
          yearDir <- runsByDate.listFiles().toStream.sortBy(-_.getName.toInt).dropWhile(_.getName.toInt > from.getYear)
          dayDir <- yearDir
            .listFiles()
            .toStream
            .sortBy(-_.getName.toInt)
            .dropWhile(dir => yearDir.getName.toInt == from.getYear && dir.getName.toInt > from.getDayOfYear)
          secondDir <- dayDir
            .listFiles()
            .toStream
            .sortBy(-_.getName.toInt)
            .dropWhile(_.getName.toInt > from.toEpochSecond(ZoneOffset.UTC))
          runId <- secondDir.list().toStream

          runInfo = RunInfo(definition, RunId(runId))
          if OrchestraConfig.statusFile(runInfo).toFile.exists()
          startAt <- ARunStatus.history(runInfo).headOption.map {
            case status: Triggered => status.at
            case status: Running   => status.at
            case status =>
              throw new IllegalStateException(s"$status is not of status type ${classOf[Triggered].getName}")
          }

          paramFile = OrchestraConfig.paramsFile(runInfo).toFile
          paramValues = if (paramFile.exists())
            decode[ParamValues](Source.fromFile(paramFile).mkString).fold(throw _, identity)
          else HNil.asInstanceOf[ParamValues]

          tagsFile = OrchestraConfig.tagsDir(definition.id).toFile
          if tagsFile.exists()
          tags = tagsFile.list().toSeq
        } yield
          (runInfo.runId, startAt, paramValues, tags, ARunStatus.current(runInfo), AStageStatus.history(runInfo.runId))

        runs.take(page.size)
      }
    }

    private[orchestra] def apiRoute(implicit ec: ExecutionContext): Route =
      path(definition.id.name / Segments) { segments =>
        entity(as[String]) { entity =>
          val body = AutowireServer.read[Map[String, String]](entity)
          val request = definition.Api.router(ApiServer).apply(Core.Request(segments, body))
          onSuccess(request)(complete(_))
        }
      }
  }
}
