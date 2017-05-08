package com.goyeau.orchestration

import com.goyeau.orchestra._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

object Main extends Orchestra {

  val emptyTask = Task('emptyTask) {
    println("empty")
  }

  val oneParamTask = Task('oneParamTask)(Param[String]("version")) { v =>
    println(v)
  }

  val deployBackend = Task('deployBackend)(Param[String]("version", Some("12")), RunId) {
    case (version, runId) =>
      println(version + runId)
  }

  val board = FolderBoard("Drivetribe")(
    FolderBoard("Operation")(
      FolderBoard("Staging")(
        SingleTaskBoard("DeployBackend", deployBackend)
      )
    ),
    FolderBoard("Infrastructure")(
      FolderBoard("Staging")(
        SingleTaskBoard("Create", oneParamTask)
      )
    )
  )
}
