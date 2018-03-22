package com.drivetribe.orchestra

import scala.language.implicitConversions

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient

import com.drivetribe.orchestra.filesystem.DirectoryUtils
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import com.drivetribe.orchestra.kubernetes.Kubernetes
import com.drivetribe.orchestra.utils._

trait AutoTuple1 {
  implicit def autoTuple1[T](o: T): Tuple1[T] = Tuple1(o)
}

object AsyncDsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with DirectoryUtils
    with Triggers
    with Stages
    with AsyncShells {
  override implicit lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override lazy val elasticsearchClient: HttpClient = Elasticsearch.client
}

object Dsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with DirectoryUtils
    with Triggers
    with Stages
    with Shells {
  override implicit lazy val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  override lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  override lazy val elasticsearchClient: HttpClient = Elasticsearch.client
}
