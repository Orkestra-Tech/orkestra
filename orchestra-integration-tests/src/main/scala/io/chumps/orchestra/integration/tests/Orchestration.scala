package io.chumps.orchestra.integration.tests

import scala.concurrent.duration._

import io.chumps.orchestra.AsyncDsl._
import io.chumps.orchestra.Orchestra
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.JobId

object Orchestration extends Orchestra {
  lazy val jobRunners = Set(SomeJob.jobRunner)
}

object SomeJob {
  lazy val job = Job[() => Unit](JobId("someJob"), "Some Job")()

  lazy val jobRunner = JobRunner(job) { implicit workDir => () =>
    println("Start")
    Thread.sleep(3.seconds.toMillis)
    println("Done")
  }
}
