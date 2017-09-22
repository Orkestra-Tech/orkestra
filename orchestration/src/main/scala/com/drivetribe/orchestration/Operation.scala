package com.drivetribe.orchestration

import com.drivetribe.orchestration.backend._
import com.drivetribe.orchestration.frontend.DeployFrontend
import com.goyeau.orchestra.FolderBoard
import com.goyeau.orchestra.github.BranchTrigger

object Operation {

  lazy val board = FolderBoard("Operation")(
    Environment.values.map(environmentBoard) :+ SqlCopy.board: _*
  )

  private def environmentBoard(environment: Environment) = {
    val biColourBoards =
      if (environment.isBiColour) Seq(SwitchActiveColour.board(environment))
      else Seq.empty

    val prodAndStagingBoards =
      if (environment.isProd) Seq(FlinkCheckpoints.board(environment))
      else Seq.empty

    val prodBoards =
      if (environment.isInstanceOf[Environment.Prod.type]) Seq(Spamatron.board)
      else Seq.empty

    FolderBoard(environment.toString)(
      Seq(
        BuildAndDeployBackend.board(environment),
        DeployFrontend.board(environment),
        DeployBackend.board(environment),
        DeployRestApi.board(environment),
        DeployFlinkJob.board(environment)
      ) ++ biColourBoards ++ prodAndStagingBoards ++ prodBoards: _*
    )
  }

  lazy val jobs = Environment.values.flatMap { environment =>
    val biColourJobs =
      if (environment.isBiColour) Seq(SwitchActiveColour.job(environment))
      else Seq.empty

    val prodAndStagingBoards =
      if (environment.isProd) Seq(FlinkCheckpoints.job(environment))
      else Seq.empty

    Seq(
      BuildAndDeployBackend.job(environment),
      DeployFrontend.job(environment),
      DeployBackend.job(environment),
      DeployRestApi.job(environment),
      DeployFlinkJob.job(environment)
    ) ++ biColourJobs ++ prodAndStagingBoards
  } :+ SqlCopy.job :+ Spamatron.job

  lazy val githubTriggers = Environment.values.flatMap { environment =>
    val repo = "drivetribe/backend"
    Seq(
      BranchTrigger(repo, environment.entryName, BuildAndDeployBackend.job(environment))
    )
  }
}
