package io.chumps.orchestra

import java.io.IOException
import java.nio.file.Paths
import java.util.UUID

import scala.io.Source

object OrchestraConfig {
  def apply(envVar: String) = Option(System.getenv(s"ORCHESTRA_$envVar")).filter(_.nonEmpty)

  val workspace = OrchestraConfig("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = OrchestraConfig("DATA").getOrElse(System.getProperty("user.home"))
  lazy val port =
    OrchestraConfig("PORT").map(_.toInt).getOrElse(throw new IllegalStateException("ORCHESTRA_PORT should be set"))
  lazy val githubPort = OrchestraConfig("GITHUB_PORT")
    .map(_.toInt)
    .getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_PORT should be set"))
  val runInfo = OrchestraConfig("RUN_INFO").map(runInfoJson => RunInfo.decodeWithFallbackRunId(runInfoJson, jobUid))
  val kubeUri =
    OrchestraConfig("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set"))
  val podName =
    OrchestraConfig("POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_POD_NAME should be set"))
  val namespace =
    OrchestraConfig("NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_NAMESPACE should be set"))

  lazy val jobUid = {
    val controllerUidRegex = """controller-uid="(.+)"""".r
    Source
      .fromFile(Paths.get(OrchestraConfig.downwardApi.toString, "labels").toFile)
      .getLines()
      .collectFirst {
        case controllerUidRegex(jobUid) => UUID.fromString(jobUid)
      }
      .getOrElse(throw new IOException("Cannot find label controller-uid"))
  }

  lazy val downwardApi = Paths.get("/var/run/downward-api")
  val jobsDirName = "jobs"
  val runsDirName = "runs"
  def runDir(runId: UUID) = Paths.get(home, runsDirName, runId.toString)
  def logsFile(runId: UUID) = Paths.get(runDir(runId).toString, "logs")
  def stagesFile(runId: UUID) = Paths.get(runDir(runId).toString, "stages")
  def jobDir(jobId: Symbol) = Paths.get(home, jobsDirName, jobId.name)
  def jobRunsDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, runsDirName)
  def jobRunDir(runInfo: RunInfo) = Paths.get(jobRunsDir(runInfo.job.id).toString, runInfo.runId.toString)
  def runsByDateDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, "runsByDate")
  def tagsDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, "tags")
  def tagDir(jobId: Symbol, tag: String) = Paths.get(tagsDir(jobId).toString, tag)
  def statusFile(runInfo: RunInfo) = Paths.get(jobRunDir(runInfo).toString, "status")
  def paramsFile(runInfo: RunInfo) = Paths.get(jobRunDir(runInfo).toString, "params")
}