package com.goyeau.orchestration

import com.goyeau.orchestra.FolderBoard

object Infrastructure {

  val board = FolderBoard("Infrastructure")(
    Environment.values.map(environmentBoard): _*
  )

  def environmentBoard(environment: Environment) = FolderBoard(environment.entryName)(
    CreateEnvironment.board(environment)
  )
}
