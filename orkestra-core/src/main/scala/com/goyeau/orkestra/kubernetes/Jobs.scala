package com.goyeau.orkestra.kubernetes

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient

import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.utils.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.api.core.v1.PodSpec
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}

import com.goyeau.orkestra.model.{EnvRunInfo, RunInfo}

private[orkestra] object Jobs {

  def name(runInfo: RunInfo) =
    s"${runInfo.jobId.value.toLowerCase}-${runInfo.runId.value.toString.split("-").head}"

  def create(
    runInfo: RunInfo,
    podSpec: PodSpec
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient): Future[Unit] =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(name(runInfo)))),
        spec = Option(JobSpecs.create(masterPod, EnvRunInfo(runInfo.jobId, Option(runInfo.runId)), podSpec))
      )
      _ <- kubernetesClient.jobs.namespace(orkestraConfig.namespace).create(job)
    } yield ()

  def delete(
    runInfo: RunInfo
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient): Future[Unit] = {
    val jobs = kubernetesClient.jobs.namespace(orkestraConfig.namespace)

    jobs.list().map { jobList =>
      jobList.items
        .find(RunInfo.fromKubeJob(_) == runInfo)
        .foreach { job =>
          jobs.delete(
            job.metadata.get.name.get,
            Option(DeleteOptions(propagationPolicy = Option("Foreground"), gracePeriodSeconds = Option(0)))
          )
        }
    }
  }
}
