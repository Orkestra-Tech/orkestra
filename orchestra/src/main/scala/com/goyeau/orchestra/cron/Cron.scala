package com.goyeau.orchestra.cron

//import com.goyeau.orchestra.kubernetes.CronJob
import com.goyeau.orchestra.{Config, JVMApp}
import com.goyeau.orchestra.AkkaImplicits._
import io.fabric8.kubernetes.api.model.Job
import io.fabric8.kubernetes.client.DefaultKubernetesClient

trait Cron extends JVMApp {

  def cronTriggers: Seq[CronTrigger]

  override def main(args: Array[String]): Unit =
    super.main(args)

//    if (Config.runInfo.isEmpty) cronTriggers.foreach(CronJob.create)
}
