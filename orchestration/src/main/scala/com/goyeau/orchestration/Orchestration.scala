package com.goyeau.orchestration

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.goyeau.orchestra._
import com.goyeau.orchestra.kubernetes._
import io.circe.generic.auto._

object Orchestration extends Orchestra {

  lazy val emptyTaskDef = Job[() => Unit]('emptyJob)
  lazy val emptyTask = emptyTaskDef(() => println("empty"))

  lazy val oneParamTaskDef = Job[String => Int]('oneParamJob)
  lazy val oneParamTask =
    oneParamTaskDef(PodConfig(Container("aws", "jakesys/aws", tty = true, Seq("cat")))) { aws => v =>
      Await.ready("echo toto" ! aws, Duration.Inf)

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
