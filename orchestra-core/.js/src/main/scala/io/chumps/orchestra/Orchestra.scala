package io.chumps.orchestra

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

trait Orchestra extends OrchestraPlugin {
  override implicit def orchestraConfig: OrchestraConfig = ???
  override implicit def kubernetesClient: KubernetesClient = ???
  override implicit def elasticsearchClient: HttpClient = ???
}
