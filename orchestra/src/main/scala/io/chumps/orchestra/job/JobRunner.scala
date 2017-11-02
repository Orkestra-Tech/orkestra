package io.chumps.orchestra.job

import java.io.FileOutputStream
import java.nio.file.Files
import java.time.{Instant, LocalDateTime, ZoneOffset}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.io.Source
import scala.language.{higherKinds, implicitConversions}

import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.Route
import autowire.Core
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import io.k8s.api.core.v1.PodSpec
import shapeless._

import io.chumps.orchestra.ARunStatus._
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.kubernetes.JobUtils
import io.chumps.orchestra.model._
import io.chumps.orchestra.utils.BaseEncoders._
import io.chumps.orchestra.utils.StagesHelpers.LogsPrintStream
import io.chumps.orchestra.utils.Utils
import io.chumps.orchestra.{ARunStatus, AStageStatus, AutowireServer, OrchestraConfig}

case class JobRunner[ParamValues <: HList: Encoder: Decoder, Result: Encoder: Decoder](
  job: Job[_, ParamValues, Result],
  podSpec: PodSpec,
  func: ParamValues => Result
) {

  private[orchestra] def run(runInfo: RunInfo): Unit = {
    Utils.runInit(runInfo, Seq.empty)
    val logsOut = new LogsPrintStream(new FileOutputStream(OrchestraConfig.logsFile(runInfo.runId).toFile, true))

    Utils.withOutErr(logsOut) {
      try {
        persist[Nothing](runInfo, Running(Instant.now()))
        println(s"Running job ${job.name}")

        val paramFile = OrchestraConfig.paramsFile(runInfo).toFile
        val result = func(
          if (paramFile.exists()) decode[ParamValues](Source.fromFile(paramFile).mkString).fold(throw _, identity)
          else HNil.asInstanceOf[ParamValues]
        )

        println(s"Job ${job.name} completed")
        persist(runInfo, Success(Instant.now(), result))
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          persist[Nothing](runInfo, Failure(Instant.now(), e))
      } finally {
        logsOut.close()
        JobUtils.selfDelete()
      }
    }
  }

  private[orchestra] object ApiServer extends job.Api {
    override def trigger(runId: RunId, params: ParamValues, tags: Seq[String] = Seq.empty): Unit = {
      val runInfo = RunInfo(job.id, runId)
      if (OrchestraConfig.statusFile(runInfo).toFile.exists()) ARunStatus.current[Result](runInfo)
      else {
        Utils.runInit(runInfo, tags)
        val logsOut = new LogsPrintStream(new FileOutputStream(OrchestraConfig.logsFile(runInfo.runId).toFile, true))

        Utils.withOutErr(logsOut) {
          try {
            persist[Nothing](runInfo, Triggered(Instant.now()))
            Files.write(OrchestraConfig.paramsFile(runInfo), AutowireServer.write(params).getBytes)

            Await.result(JobUtils.create(runInfo, podSpec), Duration.Inf)
          } catch {
            case e: Throwable =>
              e.printStackTrace()
              persist[Nothing](runInfo, Failure(Instant.now(), e))
              throw e
          } finally logsOut.close()
        }
      }
    }

    override def stop(runId: RunId): Unit = {
      val runInfo = RunInfo(job.id, runId)
      persist[Nothing](runInfo, Stopped(Instant.now()))
      JobUtils.delete(runInfo)
    }

    override def tags(): Seq[String] = Seq(OrchestraConfig.tagsDir(job.id).toFile).filter(_.exists()).flatMap(_.list())

    override def history(
      page: Page[Instant]
    ): Seq[(RunId, Instant, ParamValues, Seq[String], ARunStatus[Result], Seq[AStageStatus])] = {
      val from = page.from.fold(LocalDateTime.MAX)(LocalDateTime.ofInstant(_, ZoneOffset.UTC))

      val runs = for {
        runsByDate <- Stream(OrchestraConfig.runsByDateDir(job.id).toFile)
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

        runInfo = RunInfo(job.id, RunId(runId))
        if OrchestraConfig.statusFile(runInfo).toFile.exists()
        startAt <- ARunStatus.history[Result](runInfo).headOption.map {
          case status: Triggered => status.at
          case status: Running   => status.at
          case status =>
            throw new IllegalStateException(s"$status is not of status type ${classOf[Triggered].getName}")
        }

        paramFile = OrchestraConfig.paramsFile(runInfo).toFile
        paramValues = if (paramFile.exists())
          decode[ParamValues](Source.fromFile(paramFile).mkString).fold(throw _, identity)
        else HNil.asInstanceOf[ParamValues]

        tags = for {
          tagsDir <- Seq(OrchestraConfig.tagsDir(job.id).toFile)
          if tagsDir.exists()
          tagDir <- tagsDir.listFiles()
          runId <- tagDir.list()
          if runId == runInfo.runId.value.toString
        } yield tagDir.getName
      } yield
        (runInfo.runId,
         startAt,
         paramValues,
         tags,
         ARunStatus.current[Result](runInfo),
         AStageStatus.history(runInfo.runId))

      runs.take(page.size)
    }
  }

  private[orchestra] def apiRoute(implicit ec: ExecutionContext): Route =
    path(job.id.name / Segments) { segments =>
      entity(as[String]) { entity =>
        val body = AutowireServer.read[Map[String, String]](entity)
        val request = job.Api.router(ApiServer).apply(Core.Request(segments, body))
        onSuccess(request)(complete(_))
      }
    }
}
