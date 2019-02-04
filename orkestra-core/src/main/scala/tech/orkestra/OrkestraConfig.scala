package tech.orkestra

import java.io.IOException
import java.nio.file.Paths

import scala.io.Source
import com.sksamuel.elastic4s.http.ElasticProperties
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s.Uri
import tech.orkestra.model.{EnvRunInfo, RunId, RunInfo}

case class OrkestraConfig(
  elasticsearchProperties: ElasticProperties,
  workspace: String = OrkestraConfig.defaultWorkspace,
  port: Int = OrkestraConfig.defaultBindPort,
  runInfoMaybe: Option[RunInfo] = None,
  kubeUri: Uri,
  namespace: String,
  podName: String,
  basePath: String = OrkestraConfig.defaultBasePath
) {
  lazy val runInfo = runInfoMaybe.getOrElse(throw new IllegalStateException("ORKESTRA_RUN_INFO should be set"))
}

object OrkestraConfig {
  def fromEnvVars() = OrkestraConfig(
    ElasticProperties(
      fromEnvVar("ELASTICSEARCH_URI").getOrElse(
        throw new IllegalStateException("ORKESTRA_ELASTICSEARCH_URI should be set")
      )
    ),
    fromEnvVar("WORKSPACE").getOrElse("/opt/docker/workspace"),
    fromEnvVar("BIND_PORT").fold(defaultBindPort)(_.toInt),
    fromEnvVar("RUN_INFO").map { runInfoJson =>
      decode[EnvRunInfo](runInfoJson).fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    },
    fromEnvVar("KUBE_URI")
      .fold(throw new IllegalStateException("ORKESTRA_KUBE_URI should be set"))(Uri.unsafeFromString),
    fromEnvVar("NAMESPACE").getOrElse(throw new IllegalStateException("ORKESTRA_NAMESPACE should be set")),
    fromEnvVar("POD_NAME").getOrElse(throw new IllegalStateException("ORKESTRA_POD_NAME should be set")),
    fromEnvVar("BASEPATH").getOrElse(defaultBasePath)
  )

  def fromEnvVar(envVar: String) = sys.env.get(s"ORKESTRA_$envVar").filter(_.nonEmpty)

  lazy val defaultBindPort = 8080
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
