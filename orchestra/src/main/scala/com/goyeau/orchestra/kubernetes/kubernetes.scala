package com.goyeau.orchestra

import akka.actor.ActorSystem
import akka.Done
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import scala.concurrent.Future

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}

package object kubernetes {

  implicit class ContainerProcess(val command: String) extends AnyVal {
    def !(container: Container): Future[Done] = {
      implicit val system = ActorSystem()
      implicit val materializer = ActorMaterializer()
      import system.dispatcher

      // print each incoming strict text message
      val printSink = Sink.foreach[Message] {
        case message: BinaryMessage.Strict =>
          println(message.data.utf8String)
        case message => throw new IllegalStateException(s"Unexpected message type received: $message")
      }

      // the Future[Done] is the materialized value of Sink.foreach
      // and it is completed when the stream completes
      val flow = Flow.fromSinkAndSourceMat(printSink, Source.maybe[Message])(Keep.left)

      // upgradeResponse is a Future[WebSocketUpgradeResponse] that
      // completes or fails when the connection succeeds or fails
      // and closed is a Future[Done] representing the stream completion from above
      val token = Authorization(OAuth2BearerToken(KubeJob.authToken()))
      val uri =
        s"${KubeConfig.uri.replace("http", "ws")}/api/v1/namespaces/${KubeConfig.namespace}/pods/${KubeConfig.podName}/exec?container=${container.name}&tty=true&stdin=true&stdout=true&stderr=true${command
          .split(" ")
          .map(c => s"&command=$c")
          .mkString}"

      val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(
          WebSocketRequest(
            uri,
            extraHeaders = List(token),
            subprotocol = Option("v4.channel.k8s.io")
          ),
          flow
        )

      val connected = upgradeResponse.map { upgrade =>
        // just like a regular http request we can access response status which is available via upgrade.response.status
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) Done
        else throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }

      connected.flatMap(_ => closed).andThen { case _ => system.terminate() }
    }
  }

}
