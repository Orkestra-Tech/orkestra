package com.goyeau.orchestra.cron

import com.goyeau.orchestra.kubernetes.CronJob
import com.goyeau.orchestra.{JVMApp, OrchestraConfig}
import com.goyeau.orchestra.Implicits._

trait Cron extends JVMApp {

  def cronTriggers: Seq[CronTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    if (OrchestraConfig.runInfo.isEmpty) cronTriggers.foreach(CronJob.create)
  }
}
