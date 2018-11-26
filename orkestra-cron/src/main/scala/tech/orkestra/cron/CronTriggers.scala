package tech.orkestra.cron

import cats.effect.{Effect, IO}
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger
import shapeless._
import io.circe.shapes._
import tech.orkestra.model.RunInfo
import tech.orkestra.utils.AkkaImplicits._
import tech.orkestra.utils.Elasticsearch
import tech.orkestra.OrkestraPlugin

/**
  * Mix in this trait to get support for cron triggered jobs.
  */
trait CronTriggers extends OrkestraPlugin[IO] {
  private lazy val logger = Logger(getClass)
  override lazy val F: Effect[IO] = implicitly[Effect[IO]]

  def cronTriggers: Set[CronTrigger[IO, _]]

  override def onMasterStart(kubernetesClient: KubernetesClient[IO]): IO[Unit] = {
    implicit val kubeClient: KubernetesClient[IO] = kubernetesClient
    super.onMasterStart(kubernetesClient) *>
      IO.delay(logger.info("Configuring cron jobs")) *>
      CronJobs.deleteStale(cronTriggers) *>
      CronJobs.createOrUpdate(cronTriggers)
  }

  override def onJobStart(runInfo: RunInfo): IO[Unit] =
    super.onJobStart(runInfo) *>
      (if (cronTriggers.exists(_.jobId == runInfo.jobId))
         IO.fromFuture(IO {
           elasticsearchClient
             .execute(Elasticsearch.indexRun[HNil](runInfo, HNil, Seq.empty, None).refresh(RefreshPolicy.WaitFor))
         }) *> IO.unit
       else IO.unit)
}
