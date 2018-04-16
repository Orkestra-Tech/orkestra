package com.drivetribe.orchestra.integration.tests

import scala.concurrent.duration._

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.{Orchestra, UI}
import com.drivetribe.orchestra.board.{Folder, Job}
import com.drivetribe.orchestra.cron.CronTriggers
import com.drivetribe.orchestra.github.GithubHooks
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId

object Orchestration extends Orchestra with UI with GithubHooks with CronTriggers {
  lazy val board = Folder("Integration Test")()
  lazy val jobRunners = Set(SomeJob.jobRunner)
  lazy val githubTriggers = Set.empty
  lazy val cronTriggers = Set.empty
}

object SomeJob {
  lazy val job = Job[() => Unit](JobId("someJob"), "Some Job")()

  lazy val jobRunner = JobRunner(job) { implicit workDir => () =>
    println("Start")
    Thread.sleep(3.seconds.toMillis)
    println("Done")
  }
}
