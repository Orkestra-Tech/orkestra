package com.goyeau.orchestra.integration.tests

import scala.concurrent.duration._

import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.Orchestra
import com.goyeau.orchestra.board.{Folder, JobBoard}
import com.goyeau.orchestra.cron.CronTriggers
import com.goyeau.orchestra.github.GithubHooks
import com.goyeau.orchestra.job.Job
import com.goyeau.orchestra.model.JobId

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
