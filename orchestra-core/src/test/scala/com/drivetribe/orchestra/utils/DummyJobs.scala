package com.drivetribe.orchestra.utils

import io.circe.shapes._
import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.board.JobBoard
import com.drivetribe.orchestra.job.Job
import com.drivetribe.orchestra.model.RunId
import com.drivetribe.orchestra.parameter.{Checkbox, Input}

object DummyJobs {
  def emptyJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[() => Unit](orchestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJob(implicit orchestraConfig: OrchestraConfig) = Job(emptyJobBoard)(implicit workDir => () => ())

  def emptyWithRunIdJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[RunId => Unit](orchestraConfig.runInfo.jobId, "Empty with RunId Job")()
  def emptyWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    Job(emptyWithRunIdJobBoard)(implicit workDir => runId => ())

  def oneParamJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[String => Unit](orchestraConfig.runInfo.jobId, "One Param Job")(Input[String]("Some string"))
  def oneParamJob(implicit orchestraConfig: OrchestraConfig) =
    Job(oneParamJobBoard)(implicit workDir => someString => ())

  def oneParamWithRunIdJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[(String, RunId) => Unit](orchestraConfig.runInfo.jobId, "One Param with RunId Job")(
      Input[String]("Some string")
    )
  def oneParamWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    Job(oneParamWithRunIdJobBoard)(implicit workDir => (someString, runId) => ())

  def runIdWithOneParamJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[(RunId, String) => Unit](orchestraConfig.runInfo.jobId, "RunId with One Param Job")(
      Input[String]("Some string")
    )
  def runIdWithOneParamJob(implicit orchestraConfig: OrchestraConfig) =
    Job(runIdWithOneParamJobBoard)(implicit workDir => (runId, someString) => ())

  def twoParamsJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[(String, Boolean) => Unit](orchestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsJob(implicit orchestraConfig: OrchestraConfig) =
    Job(twoParamsJobBoard)(implicit workDir => (someString, someBool) => ())

  def twoParamsWithRunIdJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[(String, Boolean, RunId) => Unit](orchestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    Job(twoParamsWithRunIdJobBoard)(implicit workDir => (someString, someBool, runId) => ())
}
