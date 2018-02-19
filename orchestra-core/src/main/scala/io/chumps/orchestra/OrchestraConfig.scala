package io.chumps.orchestra

import java.io.IOException
import java.nio.file.Paths

import scala.io.Source

import com.sksamuel.elastic4s.ElasticsearchClientUri
import io.circe.generic.auto._
import io.circe.parser._

import io.chumps.orchestra.model.{EnvRunInfo, RunId, RunInfo}

object OrchestraConfig {
  def apply(envVar: String) = sys.env.get(s"ORCHESTRA_$envVar").filter(_.nonEmpty)

  lazy val elasticsearchUri = ElasticsearchClientUri(
    OrchestraConfig("ELASTICSEARCH_URI").getOrElse(
      throw new IllegalStateException("ORCHESTRA_ELASTICSEARCH_URI should be set")
    )
  )
  lazy val workspace = OrchestraConfig("WORKSPACE").getOrElse("/opt/docker/workspace")
  lazy val port = OrchestraConfig("PORT").map(_.toInt).getOrElse(8080)
  lazy val runInfoMaybe =
    OrchestraConfig("RUN_INFO").map(
      runInfoJson =>
        decode[EnvRunInfo](runInfoJson)
          .fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    )
  lazy val runInfo = runInfoMaybe.getOrElse(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))
  lazy val kubeUri =
    OrchestraConfig("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set"))
  lazy val podName =
    OrchestraConfig("POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_POD_NAME should be set"))
  lazy val namespace =
    OrchestraConfig("NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_NAMESPACE should be set"))
  lazy val downwardApi = Paths.get("/var/run/downward-api")
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
}
