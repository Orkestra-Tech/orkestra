package com.goyeau.orchestra.cron

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.goyeau.orchestra.kubernetes.CronJobScheduler
import com.goyeau.orchestra.{JVMApp, OrchestraConfig}

trait Cron extends JVMApp {
  implicit def actorSystem: ActorSystem
  implicit def materializer: Materializer
  implicit def executionContext: ExecutionContext

  def cronTriggers: Seq[CronTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    if (OrchestraConfig.runInfo.isEmpty) cronTriggers.foreach(CronJobScheduler(_))
  }
}
