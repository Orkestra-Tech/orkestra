package com.goyeau.orchestra

import scala.sys.process

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.{Container, Kubernetes}
import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.scalalogging.Logger

trait ShellHelpers {
  private lazy val logger = Logger(getClass)
  private def runningMessage(script: String) = logger.info(s"Running: $script")

  def sh(script: String)(implicit workDir: Directory): String = {
    runningMessage(script)
    process.Process(Seq("sh", "-c", script), workDir.file).lineStream.fold("") { (acc, line) =>
      println(line)
      s"$acc\n$line"
    }
  }

  def sh(script: String, container: Container)(implicit workDir: Directory): String = {
    runningMessage(script)

    val sink = Sink.fold[String, Message]("") {
      case (acc, BinaryMessage.Strict(data)) =>
        val exitCodeRegex =
          """.*command terminated with non-zero exit code: Error executing in Docker Container: (\d+).*""".r
        data.utf8String match {
          case messageData @ exitCodeRegex(exitCode) =>
            println(messageData)
            throw new RuntimeException(
              s"Nonzero exit value: $exitCode for script '$script' in container ${container.name}"
            )
          case messageData =>
            print(messageData)
            acc + messageData
        }
      case (_, message) => throw new IllegalStateException(s"Unexpected message type received: $message")
    }

    val flow = Flow.fromSinkAndSourceMat(sink, Source.maybe[Message])(Keep.left)

    Await.result(
      Kubernetes.client
        .namespaces(OrchestraConfig.namespace)
        .pods(OrchestraConfig.podName)
        .exec(
          flow,
          Option(container.name),
          Seq("sh", "-c", s"cd ${workDir.file.getAbsolutePath} && $script"),
          stdin = true,
          tty = true
        ),
      Duration.Inf
    )
  }
}
