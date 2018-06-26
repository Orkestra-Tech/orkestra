package tech.orkestra.utils

import io.circe.shapes._
import tech.orkestra.OrkestraConfig
import tech.orkestra.board.JobBoard
import tech.orkestra.job.Job
import tech.orkestra.model.{JobId, RunId}
import tech.orkestra.parameter.{Checkbox, Input}

object DummyJobs {
  def emptyJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[() => Unit](orkestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJob(implicit orkestraConfig: OrkestraConfig) = Job(emptyJobBoard)(implicit workDir => () => ())

  def emptyJobBoard2(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[() => Unit](JobId("emptyJob2"), "Empty Job 2")()
  def emptyJob2(implicit orkestraConfig: OrkestraConfig) = Job(emptyJobBoard2)(implicit workDir => () => ())

  def emptyWithRunIdJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[RunId => Unit](orkestraConfig.runInfo.jobId, "Empty with RunId Job")()
  def emptyWithRunIdJob(implicit orkestraConfig: OrkestraConfig) =
    Job(emptyWithRunIdJobBoard)(implicit workDir => runId => ())

  def oneParamJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[String => Unit](orkestraConfig.runInfo.jobId, "One Param Job")(Input[String]("Some string"))
  def oneParamJob(implicit orkestraConfig: OrkestraConfig) =
    Job(oneParamJobBoard)(implicit workDir => someString => ())

  def oneParamWithRunIdJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[(String, RunId) => Unit](orkestraConfig.runInfo.jobId, "One Param with RunId Job")(
      Input[String]("Some string")
    )
  def oneParamWithRunIdJob(implicit orkestraConfig: OrkestraConfig) =
    Job(oneParamWithRunIdJobBoard)(implicit workDir => (someString, runId) => ())

  def runIdWithOneParamJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[(RunId, String) => Unit](orkestraConfig.runInfo.jobId, "RunId with One Param Job")(
      Input[String]("Some string")
    )
  def runIdWithOneParamJob(implicit orkestraConfig: OrkestraConfig) =
    Job(runIdWithOneParamJobBoard)(implicit workDir => (runId, someString) => ())

  def twoParamsJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[(String, Boolean) => Unit](orkestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsJob(implicit orkestraConfig: OrkestraConfig) =
    Job(twoParamsJobBoard)(implicit workDir => (someString, someBool) => ())

  def twoParamsWithRunIdJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard[(String, Boolean, RunId) => Unit](orkestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsWithRunIdJob(implicit orkestraConfig: OrkestraConfig) =
    Job(twoParamsWithRunIdJobBoard)(implicit workDir => (someString, someBool, runId) => ())
}
