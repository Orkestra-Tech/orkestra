package io.chumps.orchestra

import scala.language.implicitConversions

import io.chumps.orchestra.filesystem.DirectoryUtils
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import io.chumps.orchestra.kubernetes.Kubernetes
import io.chumps.orchestra.utils._

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
  override implicit val orchestraConfig = OrchestraConfig.fromEnvVars()
  override val kubernetesClient = Kubernetes.client
  override val elasticsearchClient = Elasticsearch.client
}

object Dsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with DirectoryUtils
    with Triggers
    with Stages
    with Shells {
  override implicit val orchestraConfig = OrchestraConfig.fromEnvVars()
  override val kubernetesClient = Kubernetes.client
  override val elasticsearchClient = Elasticsearch.client
}
