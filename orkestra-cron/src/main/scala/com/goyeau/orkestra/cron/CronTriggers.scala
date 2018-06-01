package com.goyeau.orkestra.cron

import scala.concurrent.Future

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger
import shapeless._
import io.circe.shapes._

import com.goyeau.orkestra.model.RunInfo
import com.goyeau.orkestra.utils.AkkaImplicits._
import com.goyeau.orkestra.utils.Elasticsearch
import com.goyeau.orkestra.OrkestraPlugin

/**
  * Mix in this trait to get support for cron triggered jobs.
  */
trait CronTriggers extends OrkestraPlugin {
  private lazy val logger = Logger(getClass)

  def cronTriggers: Set[CronTrigger]

  override def onMasterStart(): Future[Unit] =
    for {
      _ <- super.onMasterStart()
      _ = logger.info("Configuring cron jobs")

      _ <- CronJobs.deleteStale(cronTriggers)
      _ <- CronJobs.createOrUpdate(cronTriggers)
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
