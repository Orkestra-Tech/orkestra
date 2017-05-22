package com.goyeau.orchestration

import java.util.UUID

import scala.io.Source

import com.goyeau.orchestra._

object Orchestration extends Orchestra {

  lazy val emptyTaskDef = Job[() => Unit]('emptyTask)
  lazy val emptyTask = emptyTaskDef(() => println("empty"))

  lazy val oneParamTaskDef = Job[String => Int]('oneParamTask)
  lazy val oneParamTask = oneParamTaskDef { v =>
    Source.fromFile("")
    println(v)
    12
  }

  lazy val deployBackendDef = Job[(String, UUID) => Unit]('deployBackend)
  lazy val deployBackend = deployBackendDef((version, runId) => println(version + runId))

  lazy val registedTasks = Seq(
    emptyTask,
    oneParamTask,
    deployBackend
  )

  lazy val board = FolderBoard("Drivetribe")(
    FolderBoard("Operation")(
      FolderBoard("Staging")(
        SingleTaskBoard("DeployBackend", deployBackendDef)(Param[String]("version", defaultValue = Some("12")), RunId)
      )
    ),
    FolderBoard("Infrastructure")(
      FolderBoard("Staging")(
        SingleTaskBoard("Create", oneParamTaskDef)(Param[String]("version")),
        SingleTaskBoard("Create", emptyTaskDef)
      )
    )
  )
}

class Deploy {
  def deploy(version: String) {
    println("Deploying")
  }
}

object Deploy {
  def deploy(version: String) {
    println("Deploying")
  }
}
