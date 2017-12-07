package io.chumps.orchestra.kubernetes

import io.chumps.orchestra._
import io.chumps.orchestra.utils.AkkaImplicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.api.core.v1.PodSpec
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}

import io.chumps.orchestra.model.{EnvRunInfo, RunInfo}

object JobUtils {

  def jobName(runInfo: RunInfo) =
    s"${runInfo.jobId.name.toLowerCase}-${runInfo.runId.value.toString.split("-").head}"

  def create(runInfo: RunInfo, podSpec: PodSpec) =
    for {
      masterPod <- MasterPod.get()
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(jobName(runInfo)))),
        spec = Option(JobSpecUtils.createJobSpec(masterPod, EnvRunInfo(runInfo.jobId, Option(runInfo.runId)), podSpec)),
      )
      _ <- Kubernetes.client.jobs.namespace(OrchestraConfig.namespace).create(job)
    } yield ()

  def delete(runInfo: RunInfo) = {
    val jobs = Kubernetes.client.jobs.namespace(OrchestraConfig.namespace)

    jobs.list().map { jobList =>
      jobList.items
        .find(RunInfo.fromKubeJob(_) == runInfo)
        .foreach { job =>
          jobs(job.metadata.get.name.get)
            .delete(Option(DeleteOptions(propagationPolicy = Option("Foreground"), gracePeriodSeconds = Option(0))))
        }
    }
  }
}
