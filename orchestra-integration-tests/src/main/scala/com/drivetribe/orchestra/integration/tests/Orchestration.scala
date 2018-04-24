package com.drivetribe.orchestra.integration.tests

import scala.concurrent.duration._

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.Orchestra
import com.drivetribe.orchestra.board.{Folder, JobBoard}
import com.drivetribe.orchestra.cron.CronTriggers
import com.drivetribe.orchestra.github.GithubHooks
import com.drivetribe.orchestra.job.Job
import com.drivetribe.orchestra.model.JobId

object Orchestration extends Orchestra with GithubHooks with CronTriggers {
  lazy val board = Folder("Integration Test")(SomeJob.board)
  lazy val jobs = Set(SomeJob.job)
  lazy val githubTriggers = Set.empty
  lazy val cronTriggers = Set.empty
}

object SomeJob {
  lazy val board = JobBoard[() => Unit](JobId("someJob"), "Some Job")()

  lazy val job = Job(board) { implicit workDir => () =>
    println("Start")
    Thread.sleep(3.seconds.toMillis)
    println("Done")
  }
}
