package com.goyeau.orchestra

import java.nio.file.Paths
import java.util.UUID

object OrchestraConfig {
  def apply(envVar: String) = Option(System.getenv(s"ORCHESTRA_$envVar")).filter(_.nonEmpty)

  val workspace = OrchestraConfig("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = OrchestraConfig("DATA").getOrElse(System.getProperty("user.home"))
  val port = OrchestraConfig("PORT").map(_.toInt)
  val githubPort = OrchestraConfig("GITHUB_PORT").map(_.toInt)
  val runInfo = OrchestraConfig("RUN_INFO").map(AutowireServer.read[RunInfo])
  val kubeUri =
    OrchestraConfig("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set"))
  val podName =
    OrchestraConfig("POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_POD_NAME should be set"))
  val namespace =
    OrchestraConfig("NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_NAMESPACE should be set"))

  val jobsDirName = "jobs"
  val logsDirName = "logs"
  def logsDirPath(runId: UUID) = Paths.get(home, logsDirName, runId.toString)
  def logsFilePath(runId: UUID) = Paths.get(logsDirPath(runId).toString, "logs")
  def jobDirPath(jobId: Symbol) = Paths.get(home, jobsDirName, jobId.name)
  def runDirPath(runInfo: RunInfo) = Paths.get(jobDirPath(runInfo.jobId).toString, runInfo.runId.toString)
  def statusFilePath(runInfo: RunInfo) = Paths.get(runDirPath(runInfo).toString, "status")
  def paramsFilePath(runInfo: RunInfo) = Paths.get(runDirPath(runInfo).toString, "params")
}
