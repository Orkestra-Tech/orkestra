package com.drivetribe.orchestra.cron

import scala.concurrent.Future

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger
import shapeless._
import io.circe.shapes._

import com.drivetribe.orchestra.model.RunInfo
import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.drivetribe.orchestra.utils.Elasticsearch
import com.drivetribe.orchestra.OrchestraPlugin

/**
  * Mix in this trait to get support for cron triggered jobs.
  */
trait CronTriggers extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)

  def cronTriggers: Set[CronTrigger]

  override def onMasterStart(): Future[Unit] =
    for {
      _ <- super.onMasterStart()
      _ = logger.info("Configuring cron jobs")

      _ <- Crons.deleteStaleCronJobs(cronTriggers)
      _ <- Crons.applyCronJobs(cronTriggers)
    } yield ()

  override def onJobStart(runInfo: RunInfo): Future[Unit] =
    for {
      _ <- super.onJobStart(runInfo)
      _ <- if (cronTriggers.exists(_.job.board.id == runInfo.jobId))
        elasticsearchClient
          .execute(Elasticsearch.indexRun[HNil](runInfo, HNil, Seq.empty, None).refresh(RefreshPolicy.WaitFor))
      else Future.unit
    } yield ()
}
