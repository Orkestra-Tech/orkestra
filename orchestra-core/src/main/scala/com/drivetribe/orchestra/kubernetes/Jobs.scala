package com.drivetribe.orchestra.kubernetes

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.utils.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.api.core.v1.PodSpec
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}

import com.drivetribe.orchestra.model.{EnvRunInfo, RunInfo}

private[orchestra] object Jobs {

  def name(runInfo: RunInfo) =
    s"${runInfo.jobId.value.toLowerCase}-${runInfo.runId.value.toString.split("-").head}"

  def create(runInfo: RunInfo, podSpec: PodSpec)(implicit orchestraConfig: OrchestraConfig,
                                                 kubernetesClient: KubernetesClient): Future[Unit] =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(name(runInfo)))),
        spec = Option(JobSpecs.create(masterPod, EnvRunInfo(runInfo.jobId, Option(runInfo.runId)), podSpec)),
      )
      _ <- kubernetesClient.jobs.namespace(orchestraConfig.namespace).create(job)
    } yield ()

  def delete(runInfo: RunInfo)(implicit orchestraConfig: OrchestraConfig,
                               kubernetesClient: KubernetesClient): Future[Unit] = {
    val jobs = kubernetesClient.jobs.namespace(orchestraConfig.namespace)

    jobs.list().map { jobList =>
      jobList.items
        .find(RunInfo.fromKubeJob(_) == runInfo)
        .foreach { job =>
          jobs.delete(job.metadata.get.name.get,
                      Option(DeleteOptions(propagationPolicy = Option("Foreground"), gracePeriodSeconds = Option(0))))
        }
    }
  }
}
