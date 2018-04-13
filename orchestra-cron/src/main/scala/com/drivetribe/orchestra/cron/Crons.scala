package com.drivetribe.orchestra.cron

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import shapeless.HNil

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.kubernetes.{JobSpecs, MasterPod}
import com.drivetribe.orchestra.model.{EnvRunInfo, JobId}
import com.drivetribe.orchestra.utils.AkkaImplicits._

private[cron] object Crons {
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
