package io.chumps.orchestra.cron

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.Logger
import shapeless._
import io.circe.shapes._

import io.chumps.orchestra.model.RunInfo
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.utils.Elasticsearch
import io.chumps.orchestra.{OrchestraConfig, OrchestraPlugin}

trait CronTriggers extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)
  protected implicit val orchestraConfig: OrchestraConfig
  protected implicit val kubernetesClient: KubernetesClient
  protected implicit val elasticsearchClient: HttpClient

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
      _ <- if (cronTriggers.exists(_.jobRunner.job.id == runInfo.jobId))
        elasticsearchClient
          .execute(Elasticsearch.indexRun[HNil](runInfo, HNil, Seq.empty, None).refresh(RefreshPolicy.WaitFor))
      else Future.unit
    } yield ()
}
