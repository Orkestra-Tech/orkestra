package tech.orkestra.cron

import scala.concurrent.Future
import com.goyeau.kubernetes.client.KubernetesClient
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobList, CronJobSpec, JobTemplateSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import tech.orkestra.OrkestraConfig
import tech.orkestra.kubernetes.{JobSpecs, MasterPod}
import tech.orkestra.model.{EnvRunInfo, JobId}
import tech.orkestra.utils.AkkaImplicits._

private[cron] object CronJobs {
  private lazy val logger = Logger(getClass)

  private def cronJobName(jobId: JobId) = jobId.value.toLowerCase

  def deleteStale(
    cronTriggers: Set[CronTrigger[_]]
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient): Future[Unit] =
    for {
      currentCronJobs <- kubernetesClient.cronJobs.namespace(orkestraConfig.namespace).list()
      currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
      cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.job.board.id))
      jobsToRemove = currentCronJobNames.diff(cronJobNames)
      _ <- Future.traverse(jobsToRemove) { cronJobName =>
        logger.debug(s"Deleting cronjob $cronJobName")
        kubernetesClient.cronJobs.namespace(orkestraConfig.namespace).delete(cronJobName)
      }
    } yield ()

  def createOrUpdate(
    cronTriggers: Set[CronTrigger[_]]
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient): Future[Unit] =
    for {
      masterPod <- MasterPod.get()
      _ <- Future.traverse(cronTriggers) { cronTrigger =>
        val cronJob = CronJob(
          metadata = Option(ObjectMeta(name = Option(cronJobName(cronTrigger.job.board.id)))),
          spec = Option(
            CronJobSpec(
              schedule = cronTrigger.schedule,
              jobTemplate = JobTemplateSpec(
                spec = Option(
                  JobSpecs
                    .create(masterPod, EnvRunInfo(cronTrigger.job.board.id, None), cronTrigger.podSpecWithDefaultParams)
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

  def list()(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient): Future[CronJobList] =
    kubernetesClient.cronJobs
      .namespace(orkestraConfig.namespace)
      .list()
}
