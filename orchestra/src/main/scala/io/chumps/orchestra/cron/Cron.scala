package io.chumps.orchestra.cron

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import io.circe.generic.auto._

import io.chumps.orchestra.{JVMApp, OrchestraConfig}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.kubernetes.{JobSpecUtils, Kubernetes, MasterPod}
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.api.core.v1.Pod
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta

trait Cron extends JVMApp {

  private lazy val logger = Logger(getClass)

  def cronTriggers: Set[CronTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    if (OrchestraConfig.runInfoMaybe.isEmpty)
      Await.result(
        for {
          masterPod <- MasterPod.get()
          currentCronJobs <- Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace).list()
          currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
        } yield {
          deleteStaleCronJobs(currentCronJobNames)
          applyCronJobs(masterPod, currentCronJobNames)
        },
        Duration.Inf
      )
  }

  private def deleteStaleCronJobs(currentCronJobNames: Set[String]) = {
    val cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.jobRunner.job.id))
    val jobsToRemove = currentCronJobNames.diff(cronJobNames)
    jobsToRemove.foreach { cronJobName =>
      Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace)(cronJobName).delete()
      logger.debug(s"Deleting cronjob $cronJobName")
    }
  }

  private def applyCronJobs(masterPod: Pod, currentCronJobNames: Set[String]) =
    cronTriggers.foreach { cronTrigger =>
      val newCronJobName = cronJobName(cronTrigger.jobRunner.job.id)
      val cronJob = CronJob(
        metadata = Option(ObjectMeta(name = Option(newCronJobName))),
        spec = Option(
          CronJobSpec(
            schedule = cronTrigger.schedule,
            jobTemplate = JobTemplateSpec(
              spec = Option(
                JobSpecUtils.createJobSpec(masterPod, cronTrigger.jobRunner.job.id, cronTrigger.jobRunner.podSpec)
              )
            )
          )
        )
      )
      val cronJobs = Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace)
      if (currentCronJobNames contains newCronJobName) {
        cronJobs(newCronJobName).replace(cronJob)
        logger.debug(s"Updated cronjob $newCronJobName")
      } else {
        cronJobs.create(cronJob)
        logger.debug(s"Created cronjob $newCronJobName")
      }
    }

  private def cronJobName(jobId: Symbol) = jobId.name.toLowerCase
}
