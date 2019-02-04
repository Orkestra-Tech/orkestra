package tech.orkestra.kubernetes

import java.io.File

import cats.effect.{ConcurrentEffect, Resource}

import scala.io.Source
import tech.orkestra.OrkestraConfig
import com.goyeau.kubernetes.client.KubernetesClient
import com.goyeau.kubernetes.client.KubeConfig
import org.http4s.Credentials.Token
import org.http4s.AuthScheme
import org.http4s.headers.Authorization

object Kubernetes {

  def client[F[_]: ConcurrentEffect](implicit orkestraConfig: OrkestraConfig): Resource[F, KubernetesClient[F]] =
    KubernetesClient[F](
      KubeConfig(
        server = orkestraConfig.kubeUri,
        authorization = Option(
          Authorization(
            Token(AuthScheme.Bearer, Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString)
          )
        ),
        caCertFile = Option(new File("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
      )
    )
}
