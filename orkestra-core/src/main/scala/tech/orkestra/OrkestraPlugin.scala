package tech.orkestra

import cats.Applicative
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.http.ElasticClient
import tech.orkestra.model.RunInfo

trait OrkestraPlugin[F[_]] {
  implicit protected def F: Applicative[F]
  implicit protected def orkestraConfig: OrkestraConfig
  implicit protected def elasticsearchClient: ElasticClient

  def onMasterStart(kubernetesClient: KubernetesClient[F]): F[Unit] = Applicative[F].unit
  def onJobStart(runInfo: RunInfo): F[Unit] = Applicative[F].unit
}
