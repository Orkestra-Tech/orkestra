package com.drivetribe.orchestration

import java.util.UUID

import com.drivetribe.orchestration.infrastructure.Infrastructure
import com.goyeau.orchestra.{Boards, _}
import com.goyeau.orchestra.cron.Cron
import com.goyeau.orchestra.github.Github

object Orchestration extends Jobs with Boards with Github with Cron {

  lazy val emptyTaskDef = Job[() => Unit]('emptyJob)
  lazy val emptyTask = emptyTaskDef(() => println("empty"))

  lazy val deployBackendDef = Job[(String, UUID) => Unit]('deployBackend)
  lazy val deployBackend = deployBackendDef((version, runId) => println(version + runId))

  lazy val githubTriggers = Seq(
//    BranchTrigger("drivetribe/backend", "toto", emptyTask)
  )

  lazy val cronTriggers = Seq(
//    CronTrigger("*/1 * * * *", emptyTask)
  )

  lazy val jobs = Seq(
    emptyTask,
    deployBackend
  ) ++ Operation.jobs ++ Infrastructure.jobs

  lazy val board = FolderBoard("Drivetribe")(
    Operation.board,
    Infrastructure.board
  )
}
