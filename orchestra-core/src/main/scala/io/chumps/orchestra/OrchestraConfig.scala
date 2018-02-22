package io.chumps.orchestra

import java.io.IOException
import java.nio.file.Paths

import scala.io.Source

import com.sksamuel.elastic4s.ElasticsearchClientUri
import io.circe.generic.auto._
import io.circe.parser._

import io.chumps.orchestra.model.{EnvRunInfo, RunId, RunInfo}

case class OrchestraConfig(elasticsearchUri: ElasticsearchClientUri,
                           workspace: String = OrchestraConfig.defaultWorkspace,
                           port: Int = OrchestraConfig.defaultPort,
                           runInfoMaybe: Option[RunInfo] = None,
                           kubeUri: String,
                           namespace: String,
                           podName: String,
                           basePath: String = OrchestraConfig.defaultBasePath) {
  lazy val runInfo = runInfoMaybe.getOrElse(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))
}

object OrchestraConfig {
  def fromEnvVars() = OrchestraConfig(
    ElasticsearchClientUri(
      fromEnvVar("ELASTICSEARCH_URI").getOrElse(
        throw new IllegalStateException("ORCHESTRA_ELASTICSEARCH_URI should be set")
      )
    ),
    fromEnvVar("WORKSPACE").getOrElse("/opt/docker/workspace"),
    fromEnvVar("PORT").map(_.toInt).getOrElse(defaultPort),
    fromEnvVar("RUN_INFO").map { runInfoJson =>
      decode[EnvRunInfo](runInfoJson).fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    },
    fromEnvVar("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set")),
    fromEnvVar("NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_NAMESPACE should be set")),
    fromEnvVar("POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_POD_NAME should be set")),
    fromEnvVar("BASEPATH").getOrElse(defaultBasePath)
  )

  def fromEnvVar(envVar: String) = sys.env.get(s"ORCHESTRA_$envVar").filter(_.nonEmpty)

  val defaultPort = 8080
  val defaultBasePath = ""
  val defaultWorkspace = "/opt/docker/workspace"
  val downwardApi = Paths.get("/var/run/downward-api")
  lazy val jobUid = {
    val controllerUidRegex = """controller-uid="(.+)"""".r
    Source
      .fromFile(OrchestraConfig.downwardApi.resolve("labels").toFile)
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
