package com.drivetribe.orchestra

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

import com.drivetribe.orchestra.model.RunInfo

trait OrchestraPlugin {
  protected implicit def orchestraConfig: OrchestraConfig
  protected implicit def kubernetesClient: KubernetesClient
  protected implicit def elasticsearchClient: HttpClient

  def onMasterStart(): Future[Unit] = Future.unit
  def onJobStart(runInfo: RunInfo): Future[Unit] = Future.unit
}
