package com.goyeau.orchestra

import java.net.URLEncoder

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.Done
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws._
import scala.sys.process

import com.goyeau.orchestra.io.Directory

package object kubernetes extends Implicits {

  def sh(script: String)(implicit workDir: Directory): String =
    process.Process(Seq("sh", "-c", script), workDir.file).lineStream.reduce { (acc, line) =>
      println(line)
      s"$acc\n$line"
    }

  private val exitCodeRegex =
    ".*command terminated with non-zero exit code: Error executing in Docker Container: (\\d+).*".r

  def sh(script: String, container: Container)(implicit workDir: Directory): String = {
    def printAndAccumulate(message: BinaryMessage.Strict, acc: String) = message.data.utf8String match {
      case messageData @ exitCodeRegex(exitCode) =>
        print(messageData)
        throw new RuntimeException(s"Nonzero exit value: $exitCode")
      case messageData =>
        print(messageData)
        acc + messageData
    }

    run(script, container, printAndAccumulate)
  }

  private def run(script: String, container: Container, messageFunc: (BinaryMessage.Strict, String) => String)(
    implicit workDir: Directory
  ) = {
    val shScript = URLEncoder.encode(s"""cd ${workDir.file.getAbsolutePath} && $script""", "UTF-8")
    val shCommand = s"""sh -c $shScript"""
    val commandParams = s"command=${shCommand.replaceAll("\\s", "&command=")}"
    val execParams = s"""container=${container.name}&tty=true&stdin=true&stdout=true&stderr=true&$commandParams"""
    val uri =
      s"${KubeConfig.uri.replace("http", "ws")}/api/v1/namespaces/${KubeConfig.namespace}/pods/${KubeConfig.podName}/exec?$execParams"

    val sink = Sink.fold[String, Message]("") {
      case (acc, message: BinaryMessage.Strict) => messageFunc(message, acc)
      case (_, message) => throw new IllegalStateException(s"Unexpected message type received: $message")
    }

    val (upgradeResponse, closed) =
      Http().singleWebSocketRequest(
        WebSocketRequest(
          uri,
          extraHeaders = List(Auth.header),
          subprotocol = Option("channel.k8s.io")
        ),
        Flow.fromSinkAndSourceMat(sink, Source.maybe[Message])(Keep.left)
      )

    val connected = upgradeResponse.map { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) Done
      else throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }

    Await.result(connected.flatMap(_ => upgradeResponse).flatMap(_ => closed), Duration.Inf)
  }

}
