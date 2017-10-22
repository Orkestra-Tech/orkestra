package io.chumps.orchestra.kubernetes

import io.chumps.orchestra._
import io.chumps.orchestra.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.api.core.v1.PodSpec
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}

import io.chumps.orchestra.model.RunInfo

object JobUtils {

  def jobName(runInfo: RunInfo) =
    s"${runInfo.job.id.name.toLowerCase}-${runInfo.runId.value.toString.split("-").head}"

  def create(runInfo: RunInfo, podSpec: PodSpec) =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(jobName(runInfo)))),
        spec = Option(JobSpecUtils.createJobSpec(masterPod, runInfo, podSpec)),
      )
      _ <- Kubernetes.client.jobs.namespace(OrchestraConfig.namespace).create(job)
    } yield ()

  def delete(runInfo: RunInfo) =
    Kubernetes.client.jobs
      .namespace(OrchestraConfig.namespace)(jobName(runInfo))
      .delete(Option(DeleteOptions(propagationPolicy = Option("Foreground"))))

  def selfDelete() =
    Kubernetes.client.jobs
      .namespace(OrchestraConfig.namespace)(OrchestraConfig.podName.take(OrchestraConfig.podName.lastIndexOf("-")))
      .delete(Option(DeleteOptions(propagationPolicy = Option("Foreground"))))
}
