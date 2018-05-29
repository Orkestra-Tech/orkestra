package com.goyeau.orchestra.utils

import java.net.ServerSocket

import com.sksamuel.elastic4s.ElasticsearchClientUri

import com.goyeau.orchestra.OrchestraConfig
import com.goyeau.orchestra.model.{JobId, RunId, RunInfo}

trait OrchestraConfigTest {
  val kubernetesApiPort = {
    val serverSocket = new ServerSocket(0)
    try serverSocket.getLocalPort
    finally serverSocket.close()
  }

  implicit val orchestraConfig: OrchestraConfig =
    OrchestraConfig(
      elasticsearchUri = ElasticsearchClientUri("elasticsearch://elasticsearch:9200"),
      runInfoMaybe = Option(RunInfo(JobId("someJob"), RunId.random())),
      kubeUri = s"http://localhost:$kubernetesApiPort",
      namespace = "someNamespace",
      podName = "somePod"
    )
}
