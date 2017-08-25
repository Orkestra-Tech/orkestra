package com.drivetribe.orchestration

import com.drivetribe.orchestration.backend.{DeployBackend, DeployFlinkJob, DeployRestApi, SqlCopy}
import com.drivetribe.orchestration.frontend.DeployFrontend
import com.goyeau.orchestra.FolderBoard

object Operation {

  lazy val board = FolderBoard("Operation")(
    Environment.values.filter(_.nonProd).map(environmentBoard) :+ SqlCopy.board: _*
  )

  private def environmentBoard(environment: Environment) = FolderBoard(environment.toString)(
    DeployBackend.board(environment),
    DeployRestApi.board(environment),
    DeployFlinkJob.board(environment),
    DeployFrontend.board(environment)
  )

  lazy val jobs = Environment.values.filter(_.nonProd).flatMap { environment =>
    Seq(
      DeployBackend.job(environment),
      DeployRestApi.job(environment),
      DeployFlinkJob.job(environment),
      DeployFrontend.job(environment)
    )
  } :+ SqlCopy.job
}
