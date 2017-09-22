package com.drivetribe.orchestration

import java.util.UUID

import com.drivetribe.orchestration.backend.{FlinkCheckpoints, Spamatron}
import com.drivetribe.orchestration.infrastructure.Infrastructure
import com.goyeau.orchestra.{Boards, _}
import com.goyeau.orchestra.cron.{Cron, CronTrigger}
import com.goyeau.orchestra.github.{BranchTrigger, Github}

object Orchestration extends Jobs with Boards with Github with Cron {

  lazy val emptyTaskDef = Job[() => Unit]('emptyJob)
  lazy val emptyTask = emptyTaskDef { () =>
    println("empty")
    Thread.sleep(10000)
  }

  lazy val deployBackendDef = Job[(String, UUID) => Unit]('deployBackend)
  lazy val deployBackend = deployBackendDef((version, runId) => println(version + runId))

  lazy val githubTriggers = Operation.githubTriggers

  lazy val cronTriggers = Seq(
    CronTrigger("*/5 * * * *", Spamatron.job),
    CronTrigger("*/30 * * * *", FlinkCheckpoints.job(Environment.Prod)),
    CronTrigger("0 */2 * * *", FlinkCheckpoints.job(Environment.Staging))
  )

  lazy val jobs = Operation.jobs ++ Infrastructure.jobs

  lazy val board = FolderBoard("Drivetribe")(
    Operation.board,
    Infrastructure.board
  )
}
