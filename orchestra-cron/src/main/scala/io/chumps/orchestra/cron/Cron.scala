package io.chumps.orchestra.cron

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import shapeless._
import io.circe.shapes._

import io.chumps.orchestra.kubernetes.{JobSpecs, MasterPod}
import io.chumps.orchestra.model.{EnvRunInfo, JobId, RunInfo}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.utils.Elasticsearch
import io.chumps.orchestra.{OrchestraConfig, OrchestraPlugin}

trait Cron extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)
  protected implicit val orchestraConfig: OrchestraConfig
  protected implicit val kubernetesClient: KubernetesClient
  protected implicit val elasticsearchClient: HttpClient

  def cronTriggers: Set[CronTrigger]

  override def onMasterStart(): Future[Unit] =
    for {
      _ <- super.onMasterStart()
      _ = logger.info("Configuring cron jobs")

      _ <- Cron.deleteStaleCronJobs(cronTriggers)
      _ <- Cron.applyCronJobs(cronTriggers)
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

object Cron {
  private lazy val logger = Logger(getClass)

  def deleteStaleCronJobs(cronTriggers: Set[CronTrigger])(implicit orchestraConfig: OrchestraConfig,
                                                          kubernetesClient: KubernetesClient) =
    for {
      currentCronJobs <- kubernetesClient.cronJobs.namespace(orchestraConfig.namespace).list()
      currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
      cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.jobRunner.job.id))
      jobsToRemove = currentCronJobNames.diff(cronJobNames)
      _ <- Future.traverse(jobsToRemove) { cronJobName =>
        logger.debug(s"Deleting cronjob $cronJobName")
        kubernetesClient.cronJobs.namespace(orchestraConfig.namespace).delete(cronJobName)
      }
    } yield ()

  def applyCronJobs(cronTriggers: Set[CronTrigger])(implicit orchestraConfig: OrchestraConfig,
                                                    kubernetesClient: KubernetesClient) =
    for {
      masterPod <- MasterPod.get()
      _ <- Future.traverse(cronTriggers) { cronTrigger =>
        val cronJob = CronJob(
          metadata = Option(ObjectMeta(name = Option(cronJobName(cronTrigger.jobRunner.job.id)))),
          spec = Option(
            CronJobSpec(
              schedule = cronTrigger.schedule,
              jobTemplate = JobTemplateSpec(
                spec = Option(
                  JobSpecs.create(masterPod,
                                  EnvRunInfo(cronTrigger.jobRunner.job.id, None),
                                  cronTrigger.jobRunner.podSpec(HNil))
                )
              )
            )
          )
        )

        kubernetesClient.cronJobs
          .namespace(orchestraConfig.namespace)
          .createOrUpdate(cronJob)
          .map(_ => logger.debug(s"Applied cronjob ${cronJob.metadata.get.name.get}"))
      }
    } yield ()

  def cronJobName(jobId: JobId) = jobId.value.toLowerCase
}
