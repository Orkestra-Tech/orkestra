package com.goyeau.orchestra.kubernetes

import java.io.IOException
import java.util.UUID

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import com.goyeau.orchestra.RunInfo
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import shapeless.HList

object Pod extends LazyLogging {

  def delete(podName: String)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      jobDeleteResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.DELETE,
          s"${KubeConfig.uri}/api/v1/namespaces/${KubeConfig.namespace}/pods/$podName",
          headers = List(Auth.header),
          entity = HttpEntity(ContentTypes.`application/json`, ByteString.empty)
        )
      )
      jobDeleteEntity <- jobDeleteResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (jobDeleteResponse.status.isFailure()) {
        val message = s"Deleting Kubernetes pod '$podName' failed: ${jobDeleteEntity.utf8String}"
        logger.error(message)
        throw new IOException(message)
      }
}
