package com.drivetribe.orchestration

import com.drivetribe.orchestration.backend._
import com.drivetribe.orchestration.frontend.DeployFrontend
import com.goyeau.orchestra.FolderBoard

object Operation {

  lazy val board = FolderBoard("Operation")(
    Environment.values.map(environmentBoard) :+ SqlCopy.board: _*
  )

  private def environmentBoard(environment: Environment) = {
    val biColourBoards =
      if (environment.isBiColour) Seq(SwitchActiveColour.board(environment))
      else Seq.empty

    FolderBoard(environment.toString)(
      Seq(
        DeployFrontend.board(environment),
        DeployBackend.board(environment),
        DeployRestApi.board(environment),
        DeployFlinkJob.board(environment)
      ) ++ biColourBoards: _*
    )
  }

  lazy val jobs = Environment.values.flatMap { environment =>
    val biColourJobs =
      if (environment.isBiColour) Seq(SwitchActiveColour.job(environment))
      else Seq.empty

    Seq(
      DeployFrontend.job(environment),
      DeployBackend.job(environment),
      DeployRestApi.job(environment),
      DeployFlinkJob.job(environment)
    ) ++ biColourJobs
  } :+ SqlCopy.job
}
