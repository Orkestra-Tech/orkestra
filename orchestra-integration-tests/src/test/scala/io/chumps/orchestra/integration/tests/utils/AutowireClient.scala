package com.drivetribe.orchestra.integration.tests.utils

import scala.concurrent.Future

import akka.http.scaladsl.model.{ContentTypes, HttpMethods}
import com.goyeau.kubernetesclient.KubernetesClient
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser._
import io.circe.syntax._

import com.drivetribe.orchestra.OrchestraConfig
import com.drivetribe.orchestra.utils.AkkaImplicits._

object AutowireClient {

  def apply(kubernetesClient: KubernetesClient, segment: String) =
    new autowire.Client[Json, Decoder, Encoder] {
      override def doCall(request: Request): Future[Json] =
        kubernetesClient.services
          .namespace(Kubernetes.namespace.metadata.get.name.get)
          .proxy(
            DeployOrchestration.service.metadata.get.name.get,
            HttpMethods.POST,
            s"/${(OrchestraConfig.apiSegment +: segment +: request.path).mkString("/")}",
            ContentTypes.`application/json`,
            Option(request.args.asJson.noSpaces)
          )
          .map(raw => parse(raw).fold(throw _, identity))

      override def read[T: Decoder](json: Json) = json.as[T].fold(throw _, identity)
      override def write[T: Encoder](obj: T) = obj.asJson
    }
}
