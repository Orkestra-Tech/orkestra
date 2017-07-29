package com.goyeau.orchestra

import RunInfo

object Config {
  def apply(envVar: String) = Option(System.getenv(s"ORCHESTRA_$envVar")).filter(_.nonEmpty)

  val workspace = Config("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = Config("DATA").getOrElse(System.getProperty("user.home"))
  val port = Config("PORT").map(_.toInt)
  val githubPort = Config("GITHUB_PORT").map(_.toInt)
  val runInfo = Config("RUN_INFO").map(AutowireServer.read[RunInfo])
  val kubeUri = Config("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set"))
  val podName = Config("POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_POD_NAME should be set"))
  val namespace = Config("NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_NAMESPACE should be set"))

  val jobsDirName = "jobs"
  def jobDirPath(jobId: Symbol) = s"$home/$jobsDirName/${jobId.name}"
  def runDirPath(runInfo: RunInfo) = s"${jobDirPath(runInfo.jobId)}/${runInfo.runId}"
  def statusFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/status"
  def paramsFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/params"
  def logsFilePath(runInfo: RunInfo) = s"${runDirPath(runInfo)}/logs"
}
