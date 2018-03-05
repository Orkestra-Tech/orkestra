package io.chumps.orchestra.integration.tests

import scala.concurrent.duration._

import io.chumps.orchestra.AsyncDsl._
import io.chumps.orchestra.{Orchestra, UI}
import io.chumps.orchestra.board.{Folder, Job}
import io.chumps.orchestra.cron.CronTriggers
import io.chumps.orchestra.github.GithubHooks
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.JobId

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
