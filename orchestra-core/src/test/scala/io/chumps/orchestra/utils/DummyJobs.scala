package com.drivetribe.orchestra.utils

import io.circe.shapes._

import com.drivetribe.orchestra.{board, OrchestraConfig}
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.RunId
import com.drivetribe.orchestra.parameter.{Checkbox, Input}

object DummyJobs {
  def emptyJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[() => Unit](orchestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJobRunner(implicit orchestraConfig: OrchestraConfig) = JobRunner(emptyJob)(implicit workDir => () => ())

  def emptyWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[RunId => Unit](orchestraConfig.runInfo.jobId, "Empty with RunId Job")()
  def emptyWithRunIdJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(emptyWithRunIdJob)(implicit workDir => runId => ())

  def oneParamJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[String => Unit](orchestraConfig.runInfo.jobId, "One Param Job")(Input[String]("Some string"))
  def oneParamJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(oneParamJob)(implicit workDir => someString => ())

  def oneParamWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[(String, RunId) => Unit](orchestraConfig.runInfo.jobId, "One Param with RunId Job")(
      Input[String]("Some string")
    )
  def oneParamWithRunIdJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(oneParamWithRunIdJob)(implicit workDir => (someString, runId) => ())

  def runIdWithOneParamJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[(RunId, String) => Unit](orchestraConfig.runInfo.jobId, "RunId with One Param Job")(
      Input[String]("Some string")
    )
  def runIdWithOneParamJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(runIdWithOneParamJob)(implicit workDir => (runId, someString) => ())

  def twoParamsJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[(String, Boolean) => Unit](orchestraConfig.runInfo.jobId, "Two Params Job")(Input[String]("Some string"),
                                                                                          Checkbox("Some bool"))
  def twoParamsJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(twoParamsJob)(implicit workDir => (someString, someBool) => ())

  def twoParamsWithRunIdJob(implicit orchestraConfig: OrchestraConfig) =
    board.Job[(String, Boolean, RunId) => Unit](orchestraConfig.runInfo.jobId, "Two Params Job")(
      Input[String]("Some string"),
      Checkbox("Some bool")
    )
  def twoParamsWithRunIdJobRunner(implicit orchestraConfig: OrchestraConfig) =
    JobRunner(twoParamsWithRunIdJob)(implicit workDir => (someString, someBool, runId) => ())
}
