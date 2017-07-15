package com.goyeau.orchestra

import java.net.URLEncoder

import akka.Done
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process

import com.goyeau.orchestra.io.Directory

package object kubernetes extends Implicits {

  implicit class ContainerProcess(val command: String) extends AnyVal {

    def !(implicit workDir: Directory, ec: ExecutionContext): Future[Int] =
      Future(Process(command).!)

    def !!(implicit workDir: Directory, ec: ExecutionContext): Future[String] =
      Future(Process(command).!!)

    def !>(container: Container)(implicit workDir: Directory): Future[Done] = {
      val printSink = Sink.foreach[Message] {
        case message: BinaryMessage.Strict => print(message.data.utf8String)
        case message => throw new IllegalStateException(s"Unexpected message type received: $message")
      }

      // the Future[Done] is the materialized value of Sink.foreach
      // and it is completed when the stream completes
      val flow = Flow.fromSinkAndSourceMat(printSink, Source.maybe[Message])(Keep.left)

      // upgradeResponse is a Future[WebSocketUpgradeResponse] that
      // completes or fails when the connection succeeds or fails
      // and closed is a Future[Done] representing the stream completion from above
      val shScript = URLEncoder.encode(s"""cd ${workDir.file.getAbsolutePath} && $command""", "UTF-8")
      val shCommand = s"""sh -c $shScript"""
      val commandParams = s"command=${shCommand.replaceAll("\\s", "&command=")}"
      val execParams = s"""container=${container.name}&tty=true&stdin=true&stdout=true&stderr=true&$commandParams"""
      val uri =
        s"${KubeConfig.uri.replace("http", "ws")}/api/v1/namespaces/${KubeConfig.namespace}/pods/${KubeConfig.podName}/exec?$execParams"

      val (upgradeResponse, closed) =
        Http().singleWebSocketRequest(
          WebSocketRequest(
            uri,
            extraHeaders = List(Auth.header),
            subprotocol = Option("channel.k8s.io")
          ),
          flow
        )

      val connected = upgradeResponse.map { upgrade =>
        // just like a regular http request we can access response status which is available via upgrade.response.status
        // status code 101 (Switching Protocols) indicates that server support WebSockets
        if (upgrade.response.status == StatusCodes.SwitchingProtocols) Done
        else throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }

      connected.flatMap(_ => upgradeResponse).flatMap(_ => closed)
    }
  }

}
