package tech.orkestra

import scala.concurrent.Future

import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

import tech.orkestra.model.RunInfo

trait OrkestraPlugin {
  implicit protected def orkestraConfig: OrkestraConfig
  implicit protected def kubernetesClient: KubernetesClient
  implicit protected def elasticsearchClient: HttpClient

  def onMasterStart(): Future[Unit] = Future.unit
  def onJobStart(runInfo: RunInfo): Future[Unit] = Future.unit
}
