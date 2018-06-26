package tech.orkestra.integration.tests

import scala.concurrent.duration._
import tech.orkestra.Dsl._
import tech.orkestra.OrkestraServer
import tech.orkestra.board.{Folder, JobBoard}
import tech.orkestra.cron.CronTriggers
import tech.orkestra.github.GithubHooks
import tech.orkestra.job.Job
import tech.orkestra.model.JobId

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
