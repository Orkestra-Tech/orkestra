package com.drivetribe.orchestra

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

import com.drivetribe.orchestra.model.RunInfo

trait OrchestraPlugin {
  implicit protected def orchestraConfig: OrchestraConfig
  implicit protected def kubernetesClient: KubernetesClient
  implicit protected def elasticsearchClient: HttpClient

  def onMasterStart(): Future[Unit] = Future.unit
  def onJobStart(runInfo: RunInfo): Future[Unit] = Future.unit
}
