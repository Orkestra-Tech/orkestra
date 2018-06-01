package com.goyeau.orkestra.utils

import java.net.ServerSocket

import com.sksamuel.elastic4s.ElasticsearchClientUri

import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.model.{JobId, RunId, RunInfo}

trait OrkestraConfigTest {
  val kubernetesApiPort = {
    val serverSocket = new ServerSocket(0)
    try serverSocket.getLocalPort
    finally serverSocket.close()
  }

  implicit val orkestraConfig: OrkestraConfig =
    OrkestraConfig(
      elasticsearchUri = ElasticsearchClientUri("elasticsearch://elasticsearch:9200"),
      runInfoMaybe = Option(RunInfo(JobId("someJob"), RunId.random())),
      kubeUri = s"http://localhost:$kubernetesApiPort",
      namespace = "someNamespace",
      podName = "somePod"
    )
}
