package com.goyeau.orkestra.integration.tests

import scala.concurrent.duration._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.OrkestraServer
import com.goyeau.orkestra.board.{Folder, JobBoard}
import com.goyeau.orkestra.cron.CronTriggers
import com.goyeau.orkestra.github.GithubHooks
import com.goyeau.orkestra.job.Job
import com.goyeau.orkestra.model.JobId

object Orkestra extends OrkestraServer with GithubHooks with CronTriggers {
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
