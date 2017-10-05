package io.chumps.orchestra.kubernetes

import io.chumps.orchestra._
import io.chumps.orchestra.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}
import shapeless.HList

object JobUtils {

  private def jobName(runInfo: RunInfo) =
    s"orchestra-${runInfo.jobId.name.toLowerCase}-${runInfo.runId.toString.split("-").head}"

  def create[Containers <: HList](runInfo: RunInfo, podConfig: PodConfig[Containers]) =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(jobName(runInfo)))),
        spec = Option(JobSpecUtils.createJobSpec(masterPod, runInfo, podConfig))
      )
      _ <- Kubernetes.client.jobs.namespace(OrchestraConfig.namespace).create(job)
    } yield ()

  def delete(runInfo: RunInfo) =
    Kubernetes.client.jobs
      .namespace(OrchestraConfig.namespace)(jobName(runInfo))
      .delete(Option(DeleteOptions(propagationPolicy = Option("Background"))))

  def selfDelete() =
    Kubernetes.client.jobs
      .namespace(OrchestraConfig.namespace)(OrchestraConfig.podName.take(OrchestraConfig.podName.lastIndexOf("-")))
      .delete(Option(DeleteOptions(propagationPolicy = Option("Background"))))
}
