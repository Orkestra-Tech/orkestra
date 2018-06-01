package com.goyeau.orkestra

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

import com.goyeau.orkestra.model.RunInfo

trait OrkestraPlugin {
  implicit protected def orkestraConfig: OrkestraConfig
  implicit protected def kubernetesClient: KubernetesClient
  implicit protected def elasticsearchClient: HttpClient

  def onMasterStart(): Future[Unit] = Future.unit
  def onJobStart(runInfo: RunInfo): Future[Unit] = Future.unit
}
