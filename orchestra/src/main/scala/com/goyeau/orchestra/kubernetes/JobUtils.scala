package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra._
import com.goyeau.orchestra.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import shapeless.HList

object JobUtils {

  def jobName(runInfo: RunInfo) =
    s"orchestra-${runInfo.jobId.name.toLowerCase}-${runInfo.runId.toString.split("-").head}"

  def create[Containers <: HList](runInfo: RunInfo, podConfig: PodConfig[Containers]) =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(jobName(runInfo)))),
        spec = Option(JobSpecUtils.createJobSpec(masterPod, runInfo, podConfig))
      )
      _ <- Kubernetes.client.namespaces(OrchestraConfig.namespace).jobs.create(job)
    } yield ()

  def delete(runInfo: RunInfo) =
    for {
      _ <- Kubernetes.client.namespaces(OrchestraConfig.namespace).jobs(jobName(runInfo)).delete()
      _ <- Kubernetes.client.namespaces(OrchestraConfig.namespace).pods(OrchestraConfig.podName).delete()
    } yield ()
}
