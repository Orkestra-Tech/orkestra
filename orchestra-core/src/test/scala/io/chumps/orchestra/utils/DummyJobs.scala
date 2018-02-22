package io.chumps.orchestra.utils

import io.circe.shapes._

import io.chumps.orchestra.{board, OrchestraConfig}
import io.chumps.orchestra.job.JobRunner

object DummyJobs {
  def emptyJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[() => Unit](orchestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJobRunner(implicit orchestraConfig: OrchestraConfig) = JobRunner(emptyJob)(implicit workDir => () => ())
}
