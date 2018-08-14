package tech.orkestra.utils

import io.circe.shapes._
import tech.orkestra.OrkestraConfig
import tech.orkestra.board.JobBoard
import tech.orkestra.job.Job
import tech.orkestra.model.JobId
import tech.orkestra.parameter.{Checkbox, Input}

object DummyJobs {
  def emptyJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[() => Unit](orkestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJob(implicit orkestraConfig: OrkestraConfig) = Job(emptyJobBoard)(implicit workDir => () => ())

  def emptyJobBoard2(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[() => Unit](JobId("emptyJob2"), "Empty Job 2")()
  def emptyJob2(implicit orkestraConfig: OrkestraConfig) = Job(emptyJobBoard2)(implicit workDir => () => ())

  def oneParamJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[String => Unit](orkestraConfig.runInfo.jobId, "One Param Job")(Input[String]("Some string"))
  def oneParamJob(implicit orkestraConfig: OrkestraConfig) =
    Job(oneParamJobBoard)(implicit workDir => someString => ())

  def twoParamsJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[(String, Boolean) => Unit](orkestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsJob(implicit orkestraConfig: OrkestraConfig) =
    Job(twoParamsJobBoard)(implicit workDir => (someString, someBool) => ())
}
