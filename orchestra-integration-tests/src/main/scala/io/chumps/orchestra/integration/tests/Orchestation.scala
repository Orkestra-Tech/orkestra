package io.chumps.orchestra.integration.tests

import io.chumps.orchestra.Dsl._
import io.chumps.orchestra.Jobs
import io.chumps.orchestra.board.Job
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.JobId

object Orchestation extends Jobs {
  lazy val jobRunners = Set(SomeJob.jobRunner)
}

object SomeJob {
  lazy val job = Job[() => Unit](JobId("someJob"), "Some Job")()

  lazy val jobRunner = JobRunner(job) { implicit workDir => () =>
    println("Done")
  }
}
