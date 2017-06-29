package com.goyeau.orchestra.kubernetes

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import io.circe.parser.parse

object MasterPod {

  def get()(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      masterPodResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.GET,
          s"${KubeConfig.uri}/api/v1/namespaces/${KubeConfig.namespace}/pods/${KubeConfig.podName}",
          headers = List(Auth.header)
        )
      )
      masterPodEntity <- masterPodResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield parse(masterPodEntity.utf8String).fold(throw _, identity)
}
