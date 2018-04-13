package com.drivetribe.orchestra

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

/**
  * Mix in this trait to create the Orchestra job server.
  */
trait Orchestra extends OrchestraPlugin {
  override implicit def orchestraConfig: OrchestraConfig = ???
  override implicit def kubernetesClient: KubernetesClient = ???
  override implicit def elasticsearchClient: HttpClient = ???
}
