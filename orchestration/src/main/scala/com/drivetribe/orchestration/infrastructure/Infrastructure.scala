package com.drivetribe.orchestration.infrastructure

import com.drivetribe.orchestration.Environment
import com.goyeau.orchestra.FolderBoard

object Infrastructure {

  lazy val board = FolderBoard("Infrastructure")(
    Environment.values.filter(_.nonProd).map(environmentBoard): _*
  )

  private def environmentBoard(environment: Environment) = FolderBoard(environment.toString)(
    CreateEnvironment.board(environment),
    DestroyEnvironment.board(environment)
  )

  lazy val jobs = Environment.values.filter(_.nonProd).flatMap { environment =>
    Seq(
      CreateEnvironment.job(environment),
      DestroyEnvironment.job(environment)
    )
  }
}
