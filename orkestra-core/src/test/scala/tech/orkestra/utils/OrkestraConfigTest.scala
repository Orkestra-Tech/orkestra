package tech.orkestra.utils

import java.net.ServerSocket

import com.sksamuel.elastic4s.http.ElasticProperties
import tech.orkestra.OrkestraConfig
import tech.orkestra.model.{JobId, RunId, RunInfo}

trait OrkestraConfigTest {
  val kubernetesApiPort = {
    val serverSocket = new ServerSocket(0)
    try serverSocket.getLocalPort
    finally serverSocket.close()
  }

  implicit val orkestraConfig: OrkestraConfig =
    OrkestraConfig(
      elasticsearchProperties = ElasticProperties("elasticsearch://elasticsearch:9200"),
      runInfoMaybe = Option(RunInfo(JobId("someJob"), RunId.random())),
      kubeUri = s"http://localhost:$kubernetesApiPort",
      namespace = "someNamespace",
      podName = "somePod"
    )
}
