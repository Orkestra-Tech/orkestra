package tech.orkestra.cron

import cats.effect.Sync
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobList, CronJobSpec, JobTemplateSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import tech.orkestra.OrkestraConfig
import tech.orkestra.kubernetes.{JobSpecs, MasterPod}
import tech.orkestra.model.{EnvRunInfo, JobId}

private[cron] object CronJobs {
  private lazy val logger = Logger(getClass)

  private def cronJobName(jobId: JobId) = jobId.value.toLowerCase

  def deleteStale[F[_]: Sync](
    cronTriggers: Set[CronTrigger[F, _]]
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[Unit] =
    for {
      currentCronJobs <- kubernetesClient.cronJobs.namespace(orkestraConfig.namespace).list
      currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
      cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.jobId))
      jobsToRemove = currentCronJobNames.diff(cronJobNames)
      _ <- jobsToRemove.toList.traverse { cronJobName =>
        logger.debug(s"Deleting cronjob $cronJobName")
        kubernetesClient.cronJobs.namespace(orkestraConfig.namespace).delete(cronJobName)
      }
    } yield ()

  def createOrUpdate[F[_]: Sync](
    cronTriggers: Set[CronTrigger[F, _]]
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[Unit] =
    for {
      masterPod <- MasterPod.get
      _ <- cronTriggers.toList.traverse { cronTrigger =>
        val cronJob = CronJob(
          metadata = Option(ObjectMeta(name = Option(cronJobName(cronTrigger.jobId)))),
          spec = Option(
            CronJobSpec(
              schedule = cronTrigger.schedule,
              jobTemplate = JobTemplateSpec(
                spec = Option(
                  JobSpecs.create(masterPod, EnvRunInfo(cronTrigger.jobId, None), cronTrigger.podSpecWithDefaultParams)
                )
              )
            )
          )
        )

        kubernetesClient.cronJobs
          .namespace(orkestraConfig.namespace)
          .createOrUpdate(cronJob)
          .map(_ => logger.debug(s"Applied cronjob ${cronJob.metadata.get.name.get}"))
      }
    } yield ()

  def list[F[_]](implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[CronJobList] =
    kubernetesClient.cronJobs
      .namespace(orkestraConfig.namespace)
      .list
}
