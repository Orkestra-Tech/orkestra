package com.goyeau.orchestra

import java.io.IOException

import scala.sys.process

import com.goyeau.orchestra.AkkaImplicits._
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.{Container, Kubernetes}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

trait ShellHelpers {
  private def runningMessage(script: String) = println(s"Running: $script")

  def sh(script: String)(implicit workDir: Directory): String = {
    runningMessage(script)
    process.Process(Seq("sh", "-c", script), workDir.file).lineStream.fold("") { (acc, line) =>
      println(line)
      s"$acc\n$line"
    }
  }

  def sh(script: String, container: Container)(implicit workDir: Directory): String = {
    runningMessage(script)
    val stageId = LoggingHelpers.stageVar.value

    val sink = Sink.fold[String, Message]("") {
      case (acc, BinaryMessage.Strict(data)) =>
        val exitCodeRegex =
          """.*command terminated with non-zero exit code: Error executing in Docker Container: (\d+).*""".r

        LoggingHelpers.stageVar.withValue(stageId) {
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
        }
      case (_, message) => throw new IllegalStateException(s"Unexpected message type received: $message")
    }
    val flow = Flow.fromSinkAndSourceMat(sink, Source.maybe[Message])(Keep.left)

    def exec(timeout: Duration = 1.minute, interval: Duration = 300.millis): Future[String] =
      Kubernetes.client.pods
        .namespace(OrchestraConfig.namespace)(OrchestraConfig.podName)
        .exec(
          flow,
          Option(container.name),
          Seq("sh", "-c", s"cd ${workDir.file.getAbsolutePath} && $script"),
          stdin = true,
          tty = true
        )
        .recoverWith {
          case _: IOException if timeout > 0.milli =>
            Thread.sleep(interval.toMillis)
            exec(timeout - interval, interval)
        }

    Await.result(exec(), Duration.Inf)
  }
}
