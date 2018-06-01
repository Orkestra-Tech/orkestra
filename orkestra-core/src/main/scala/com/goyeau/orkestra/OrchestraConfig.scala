package com.goyeau.orkestra

import java.io.IOException
import java.nio.file.Paths

import scala.io.Source

import com.sksamuel.elastic4s.ElasticsearchClientUri
import io.circe.generic.auto._
import io.circe.parser._

import com.goyeau.orkestra.model.{EnvRunInfo, RunId, RunInfo}

case class OrkestraConfig(
  elasticsearchUri: ElasticsearchClientUri,
  workspace: String = OrkestraConfig.defaultWorkspace,
  port: Int = OrkestraConfig.defaultPort,
  runInfoMaybe: Option[RunInfo] = None,
  kubeUri: String,
  namespace: String,
  podName: String,
  basePath: String = OrkestraConfig.defaultBasePath
) {
  lazy val runInfo = runInfoMaybe.getOrElse(throw new IllegalStateException("ORKESTRA_RUN_INFO should be set"))
}

object OrkestraConfig {
  def fromEnvVars() = OrkestraConfig(
    ElasticsearchClientUri(
      fromEnvVar("ELASTICSEARCH_URI").getOrElse(
        throw new IllegalStateException("ORKESTRA_ELASTICSEARCH_URI should be set")
      )
    ),
    fromEnvVar("WORKSPACE").getOrElse("/opt/docker/workspace"),
    fromEnvVar("PORT").map(_.toInt).getOrElse(defaultPort),
    fromEnvVar("RUN_INFO").map { runInfoJson =>
      decode[EnvRunInfo](runInfoJson).fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    },
    fromEnvVar("KUBE_URI").getOrElse(throw new IllegalStateException("ORKESTRA_KUBE_URI should be set")),
    fromEnvVar("NAMESPACE").getOrElse(throw new IllegalStateException("ORKESTRA_NAMESPACE should be set")),
    fromEnvVar("POD_NAME").getOrElse(throw new IllegalStateException("ORKESTRA_POD_NAME should be set")),
    fromEnvVar("BASEPATH").getOrElse(defaultBasePath)
  )

  def fromEnvVar(envVar: String) = sys.env.get(s"ORKESTRA_$envVar").filter(_.nonEmpty)

  lazy val defaultPort = 8080
  lazy val defaultBasePath = ""
  lazy val defaultWorkspace = "/opt/docker/workspace"
  lazy val downwardApi = Paths.get("/var/run/downward-api")
  lazy val jobUid = {
    val controllerUidRegex = """controller-uid="(.+)"""".r
    Source
      .fromFile(OrkestraConfig.downwardApi.resolve("labels").toFile)
      .getLines()
      .collectFirst {
        case controllerUidRegex(jobUid) => RunId(jobUid)
      }
      .getOrElse(throw new IOException("Cannot find label controller-uid"))
  }

  val apiSegment = "api"
  val jobSegment = "job"
  val commonSegment = "common"
}
