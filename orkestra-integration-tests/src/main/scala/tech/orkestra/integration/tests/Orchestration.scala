package tech.orkestra.integration.tests

import cats.effect.IO
import shapeless._

import scala.concurrent.duration._
import tech.orkestra.Dsl._
import tech.orkestra.OrkestraServer
import tech.orkestra.board.{Folder, JobBoard}
import tech.orkestra.cron.CronTriggers
import tech.orkestra.github.GithubHooks
import tech.orkestra.job.Job
import tech.orkestra.model.JobId

import scala.concurrent.ExecutionContext

object Orkestra extends OrkestraServer[IO] with GithubHooks with CronTriggers {
  lazy val board = Folder("Integration Test")(SomeJob.board)
  lazy val jobs = Set(SomeJob.job)
  lazy val githubTriggers = Set.empty
  lazy val cronTriggers = Set.empty
}

object SomeJob {
  implicit val timer = IO.timer(ExecutionContext.global)

  lazy val board = JobBoard(JobId("someJob"), "Some Job")(HNil)

  lazy val job = Job(board) { _ =>
  for {
      _ <- IO(println("Start"))
      _ <- IO.sleep(3.seconds)
      _ <- IO(println("Done"))
    } yield ()
  }
}
