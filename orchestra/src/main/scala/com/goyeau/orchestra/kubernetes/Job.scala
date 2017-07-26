package com.goyeau.orchestra.kubernetes

import java.util.UUID

import com.goyeau.orchestra.{Config, PodConfig, RunInfo}
import io.fabric8.kubernetes.api.model.{Job => KubeJob, ObjectMeta}
import shapeless.HList

object Job {

  private def jobName(runId: UUID) = s"orchestra-slave-$runId"

  def create[Containers <: HList](runInfo: RunInfo, podConfig: PodConfig[Containers]) = {
    val job = new KubeJob()
    job.setApiVersion("batch/v1") // @TODO Remove this together with the monkey patch of JobOperationsImpl
    job.setMetadata({
      val meta = new ObjectMeta()
      meta.setName(jobName(runInfo.runId))
      meta
    })
    job.setSpec(JobSpecUtils.createJobSpec(MasterPod.get(), runInfo, podConfig))
    Kubernetes.client.extensions.jobs.inNamespace(Config.namespace).create(job)
  }

  def delete(runInfo: RunInfo) =
    Kubernetes.client.extensions.jobs.inNamespace(Config.namespace).withName(jobName(runInfo.runId)).delete()
}
