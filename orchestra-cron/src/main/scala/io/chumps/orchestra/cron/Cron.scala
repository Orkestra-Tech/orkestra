package io.chumps.orchestra.cron

import scala.concurrent.Future

import com.sksamuel.elastic4s.RefreshPolicy
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.api.core.v1.Pod
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import shapeless._
import io.circe.shapes._

import io.chumps.orchestra.kubernetes.{JobSpecUtils, Kubernetes, MasterPod}
import io.chumps.orchestra.model.{EnvRunInfo, JobId, RunInfo}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.{Elasticsearch, OrchestraConfig, OrchestraPlugin}

trait Cron extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)

  def cronTriggers: Set[CronTrigger]

  override def onMasterStart(): Future[Unit] =
    for {
      _ <- super.onMasterStart()
      _ = logger.info("Configuring cron jobs")

      masterPod <- MasterPod.get()
      currentCronJobs <- Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace).list()
      currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
      _ <- deleteStaleCronJobs(currentCronJobNames)
      _ <- applyCronJobs(masterPod)
    } yield ()

  override def onJobStart(runInfo: RunInfo): Future[Unit] =
    for {
      _ <- super.onJobStart(runInfo)
      _ <- if (cronTriggers.exists(_.jobRunner.job.id == runInfo.jobId))
        Elasticsearch.client
          .execute(Elasticsearch.indexRun[HNil](runInfo, HNil, Seq.empty, None).refresh(RefreshPolicy.WaitFor))
      else Future.unit
    } yield ()

  private def deleteStaleCronJobs(currentCronJobNames: Set[String]) = {
    val cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.jobRunner.job.id))
    val jobsToRemove = currentCronJobNames.diff(cronJobNames)
    Future.traverse(jobsToRemove) { cronJobName =>
      logger.debug(s"Deleting cronjob $cronJobName")
      Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace).delete(cronJobName)
    }
  }

  private def applyCronJobs(masterPod: Pod) =
    Future.traverse(cronTriggers) { cronTrigger =>
      val cronJob = CronJob(
        metadata = Option(ObjectMeta(name = Option(cronJobName(cronTrigger.jobRunner.job.id)))),
        spec = Option(
          CronJobSpec(
            schedule = cronTrigger.schedule,
            jobTemplate = JobTemplateSpec(
              spec = Option(
                JobSpecUtils.createJobSpec(masterPod,
                                           EnvRunInfo(cronTrigger.jobRunner.job.id, None),
                                           cronTrigger.jobRunner.podSpec(HNil))
              )
            )
          )
        )
      )

      Kubernetes.client.cronJobs
        .namespace(OrchestraConfig.namespace)
        .createOrUpdate(cronJob)
        .map(_ => logger.debug(s"Applied cronjob ${cronJob.metadata.get.name.get}"))
    }

  private def cronJobName(jobId: JobId) = jobId.value.toLowerCase
}
