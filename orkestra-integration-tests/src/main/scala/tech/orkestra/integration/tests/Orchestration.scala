package tech.orkestra.integration.tests

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._
import java.io.File

import shapeless._

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
  lazy val board = JobBoard(JobId("someJob"), "Some Job")(HNil)

  def job(implicit timer: Timer[IO], contextShift: ContextShift[IO]) = Job(board) { _ =>
    IO(println("Start")) *>
      IO(println(new File("some-file").exists())) *>
      IO.sleep(3.seconds) *>
      IO(println("Done"))
  }
}
