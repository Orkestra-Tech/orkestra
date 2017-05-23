package com.goyeau.orchestration

import java.util.UUID

import com.goyeau.orchestra._
import io.circe.generic.auto._

object Orchestration extends Orchestra {

  lazy val emptyTaskDef = Job[() => Unit]('emptyTask)
  lazy val emptyTask = emptyTaskDef(() => println("empty"))

  lazy val oneParamTaskDef = Job[String => Int]('oneParamTask)
  lazy val oneParamTask = oneParamTaskDef { v =>
    println(v)
    12
  }

  lazy val deployBackendDef = Job[(String, UUID) => Unit]('deployBackend)
  lazy val deployBackend = deployBackendDef((version, runId) => println(version + runId))

  lazy val jobs = Seq(
    emptyTask,
    oneParamTask,
    deployBackend
  )

  lazy val board = FolderBoard("Drivetribe")(
    FolderBoard("Operation")(
      FolderBoard("Staging")(
        SingleJobBoard("DeployBackend", deployBackendDef)(Param[String]("version", defaultValue = Some("12")), RunId)
      )
    ),
    FolderBoard("Infrastructure")(
      FolderBoard("Staging")(
        SingleJobBoard("OneParam", oneParamTaskDef)(Param[String]("version")),
        SingleJobBoard("Create", emptyTaskDef)
      )
    )
  )
}
