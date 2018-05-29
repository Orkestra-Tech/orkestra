package com.goyeau.orchestra.utils

import io.circe.shapes._
import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.orchestra.board.JobBoard
import com.goyeau.orchestra.job.Job
import com.goyeau.orchestra.model.{JobId, RunId}
import com.goyeau.orchestra.parameter.{Checkbox, Input}

object DummyJobs {
  def emptyJobBoard(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[() => Unit](orchestraConfig.runInfo.jobId, "Empty Job")()
  def emptyJob(implicit orchestraConfig: OrchestraConfig) = Job(emptyJobBoard)(implicit workDir => () => ())

  def emptyJobBoard2(implicit orchestraConfig: OrchestraConfig) =
    JobBoard[() => Unit](JobId("emptyJob2"), "Empty Job 2")()
  def emptyJob2(implicit orchestraConfig: OrchestraConfig) = Job(emptyJobBoard2)(implicit workDir => () => ())

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
