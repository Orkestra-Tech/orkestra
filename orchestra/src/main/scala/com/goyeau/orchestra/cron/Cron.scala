package com.goyeau.orchestra.cron

import com.goyeau.orchestra.{JVMApp, OrchestraConfig, RunInfo}
import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.kubernetes.{JobSpecUtils, Kubernetes, MasterPod}
import io.k8s.api.batch.v2alpha1.{CronJob, CronJobSpec, JobTemplateSpec}
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta

trait Cron extends JVMApp {

  def cronTriggers: Seq[CronTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    if (OrchestraConfig.runInfo.isEmpty) MasterPod.get().map { masterPod =>
      cronTriggers.foreach { cronTrigger =>
        val runInfo = RunInfo(cronTrigger.job.definition.id, None)
        val cronJob = CronJob(
          metadata = Option(ObjectMeta(name = Option(s"orchestra-cronjob-${runInfo.jobId.name.toLowerCase}"))),
          spec = Option(
            CronJobSpec(
              schedule = cronTrigger.schedule,
              jobTemplate = JobTemplateSpec(
                spec = Option(JobSpecUtils.createJobSpec(masterPod, runInfo, cronTrigger.job.podConfig))
              )
            )
          )
        )
        Kubernetes.client.namespaces(OrchestraConfig.namespace).cronJobs.create(cronJob)
      }
    }
  }
}
