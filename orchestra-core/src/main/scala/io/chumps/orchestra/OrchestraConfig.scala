package io.chumps.orchestra

import java.io.IOException
import java.nio.file.Paths

import scala.io.Source

import akka.http.scaladsl.model.Uri
import io.circe.parser._

import io.chumps.orchestra.model.{EnvRunInfo, RunId, RunInfo}

object OrchestraConfig {
  val appName = "Orchestra"
  def apply(envVar: String) = Option(System.getenv(s"ORCHESTRA_$envVar")).filter(_.nonEmpty)

  val elasticsearchUri = OrchestraConfig("ELASTICSEARCH_URI").getOrElse(
    throw new IllegalStateException("ORCHESTRA_ELASTICSEARCH_URI should be set")
  )
  val workspace = OrchestraConfig("WORKSPACE").getOrElse("/opt/docker/workspace")
  val home = OrchestraConfig("DATA").getOrElse(System.getProperty("user.home"))
  lazy val port =
    OrchestraConfig("PORT").map(_.toInt).getOrElse(throw new IllegalStateException("ORCHESTRA_PORT should be set"))
  lazy val url =
    OrchestraConfig("URL").fold(throw new IllegalStateException("ORCHESTRA_URL should be set"))(Uri(_))
  lazy val githubPort = OrchestraConfig("GITHUB_PORT")
    .map(_.toInt)
    .getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_PORT should be set"))
  lazy val githubToken =
    OrchestraConfig("GITHUB_TOKEN").getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_TOKEN should be set"))
  val runInfoMaybe =
    OrchestraConfig("RUN_INFO").map(
      runInfoJson =>
        decode[EnvRunInfo](runInfoJson)
          .fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    )
  lazy val runInfo = runInfoMaybe.getOrElse(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))
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
        case controllerUidRegex(jobUid) => RunId(jobUid)
      }
      .getOrElse(throw new IOException("Cannot find label controller-uid"))
  }

  lazy val downwardApi = Paths.get("/var/run/downward-api")
  val jobsDirName = "jobs"
  val runsDirName = "runs"
  def runDir(runId: RunId) = Paths.get(home, runsDirName, runId.value.toString)
  def logsFile(runId: RunId) = Paths.get(runDir(runId).toString, "logs")
  def stagesFile(runId: RunId) = Paths.get(runDir(runId).toString, "stages")
  def jobDir(jobId: Symbol) = Paths.get(home, jobsDirName, jobId.name)
  def jobRunsDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, runsDirName)
  def jobRunDir(runInfo: RunInfo) = Paths.get(jobRunsDir(runInfo.jobId).toString, runInfo.runId.value.toString)
  def runsByDateDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, "runsByDate")
  def tagsDir(jobId: Symbol) = Paths.get(jobDir(jobId).toString, "tags")
  def tagDir(jobId: Symbol, tag: String) = Paths.get(tagsDir(jobId).toString, tag)
  def statusFile(runInfo: RunInfo) = Paths.get(jobRunDir(runInfo).toString, "status")
  def paramsFile(runInfo: RunInfo) = Paths.get(jobRunDir(runInfo).toString, "params")
}
