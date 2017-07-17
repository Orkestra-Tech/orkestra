package com.goyeau.orchestra

import java.io.File

object OrchestraConfig {
  val jobsDirName = "jobs" // TODO maybe we should move that somewhere else

  val workspace = Config("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = Config("DATA").getOrElse(System.getProperty("user.home"))
  val port = Config("PORT").map(_.toInt)
  val githubPort = Config("GITHUB_PORT").map(_.toInt)
  val runInfo = Config("RUN_INFO").map(AutowireServer.read[RunInfo])

  def jobDirPath(jobId: Symbol) = s"${OrchestraConfig.home}/${OrchestraConfig.jobsDirName}/${jobId.name}"
  def runDirPath(runInfo: RunInfo) = s"${jobDirPath(runInfo.jobId)}/${runInfo.runId}"
  def statusFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/status"
  def paramsFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/params"
  def logsFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/logs"
}
