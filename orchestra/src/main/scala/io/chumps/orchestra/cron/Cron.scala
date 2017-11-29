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
import shapeless.HNil

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
          applyCronJobs(masterPod)
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

  private def applyCronJobs(masterPod: Pod) =
    cronTriggers.foreach { cronTrigger =>
      val cronJob = CronJob(
        metadata = Option(ObjectMeta(name = Option(cronJobName(cronTrigger.jobRunner.job.id)))),
        spec = Option(
          CronJobSpec(
            schedule = cronTrigger.schedule,
            jobTemplate = JobTemplateSpec(
              spec = Option(
                JobSpecUtils.createJobSpec(masterPod,
                                           cronTrigger.jobRunner.job.id,
                                           cronTrigger.jobRunner.podSpec(HNil))
              )
            )
          )
        )
      )

      Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace).createOrUpdate(cronJob).foreach { _ =>
        logger.debug(s"Applied cronjob ${cronJob.metadata.get.name.get}")
      }
    }

  private def cronJobName(jobId: Symbol) = jobId.name.toLowerCase
}
