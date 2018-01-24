package io.chumps.orchestra.cron

import java.io.IOException

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.RefreshPolicy
import com.typesafe.scalalogging.Logger
import io.k8s.api.batch.v1beta1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.api.core.v1.Pod
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import shapeless._
import io.circe.shapes._

import io.chumps.orchestra.kubernetes.{JobSpecUtils, Kubernetes, MasterPod}
import io.chumps.orchestra.model.{EnvRunInfo, JobId, RunInfo}
import io.chumps.orchestra.model.Indexed.HistoryIndex
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.{Elasticsearch, OrchestraConfig, OrchestraPlugin}

trait Cron extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)

  def cronTriggers: Set[CronTrigger]

  override def onMasterStart(): Unit = {
    super.onMasterStart()
    logger.info("Configuring cron jobs")

    Await.result(
      for {
        masterPod <- MasterPod.get()
        currentCronJobs <- Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace).list()
        currentCronJobNames = currentCronJobs.items.flatMap(_.metadata).flatMap(_.name).toSet
        _ <- deleteStaleCronJobs(currentCronJobNames)
        _ <- applyCronJobs(masterPod)
      } yield (),
      1.minute
    )
  }

  override def onJobStart(runInfo: RunInfo): Unit = {
    super.onJobStart(runInfo)
    Await.result(
      for {
        runExists <- Elasticsearch.client
          .execute(exists(HistoryIndex.formatId(runInfo), HistoryIndex.index, HistoryIndex.`type`))
          .map(_.fold(failure => throw new IOException(failure.error.reason), identity).result)
        _ <- if (runExists) Future.unit
        else Elasticsearch.indexRun[HNil](runInfo, HNil, Seq.empty, None, RefreshPolicy.WaitFor)
      } yield (),
      1.minute
    )
  }

  private def deleteStaleCronJobs(currentCronJobNames: Set[String]) = {
    val cronJobNames = cronTriggers.map(cronTrigger => cronJobName(cronTrigger.jobRunner.job.id))
    val jobsToRemove = currentCronJobNames.diff(cronJobNames)
    Future.traverse(jobsToRemove) { cronJobName =>
      logger.debug(s"Deleting cronjob $cronJobName")
      Kubernetes.client.cronJobs.namespace(OrchestraConfig.namespace)(cronJobName).delete()
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
