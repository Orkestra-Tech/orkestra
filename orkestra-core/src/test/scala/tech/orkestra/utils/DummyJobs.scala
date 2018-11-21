package tech.orkestra.utils

import cats.effect.IO
import shapeless._
import tech.orkestra.Dsl._
import tech.orkestra.OrkestraConfig
import tech.orkestra.board.JobBoard
import tech.orkestra.job.Job
import tech.orkestra.model.JobId
import tech.orkestra.input.{Checkbox, Text}

object DummyJobs {
  def emptyJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard(orkestraConfig.runInfo.jobId, "Empty Job")(HNil)
  def emptyJob(implicit orkestraConfig: OrkestraConfig) =
    Job(emptyJobBoard)(_ => IO.unit)

  def emptyJobBoard2(implicit orkestraConfig: OrkestraConfig) =
    JobBoard(JobId("emptyJob2"), "Empty Job 2")(HNil)
  def emptyJob2(implicit orkestraConfig: OrkestraConfig) = Job(emptyJobBoard2)(_ => IO.unit)

  def oneParamJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard(orkestraConfig.runInfo.jobId, "One Param Job")(Text[String]("Some string") :: HNil)
  def oneParamJob(implicit orkestraConfig: OrkestraConfig) =
    Job(oneParamJobBoard) {
      case someString :: HNil =>
        IO(println(someString))
    }

  def twoParamsJobBoard(implicit orkestraConfig: OrkestraConfig) =
    JobBoard(orkestraConfig.runInfo.jobId, "Two Params Job")(
      Text[String]("Some string") ::
        Checkbox("Some bool") ::
        HNil
    )
  def twoParamsJob(implicit orkestraConfig: OrkestraConfig) =
    Job(twoParamsJobBoard) {
      case _ :: _ :: HNil =>
        IO.unit
    }
}
