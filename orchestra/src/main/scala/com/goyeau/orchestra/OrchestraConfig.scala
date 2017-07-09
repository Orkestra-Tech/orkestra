package com.goyeau.orchestra

import java.io.File

object OrchestraConfig {
  val jobsDirName = "jobs" // TODO maybe we should move that somewhere else

  val workspace = Config("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = Config("DATA").getOrElse(System.getProperty("user.home"))
  val port = Config("PORT").map(_.toInt)
  val githubPort = Config("GITHUB_PORT").map(_.toInt)
  val runInfo = Config("RUN_INFO").map(AutowireServer.read[RunInfo])
}
