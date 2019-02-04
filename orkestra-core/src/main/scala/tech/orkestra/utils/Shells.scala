package tech.orkestra.utils

import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import cats.effect._
import cats.implicits._
import com.goyeau.kubernetes.client.{KubernetesClient, KubernetesException}
import io.k8s.api.core.v1.Container
import io.k8s.apimachinery.pkg.apis.meta.v1.Status
import java.io.IOException

import scala.concurrent.duration._
import scala.sys.process.Process
import tech.orkestra.OrkestraConfig
import tech.orkestra.filesystem.Directory
import tech.orkestra.kubernetes.Kubernetes
import tech.orkestra.utils.AkkaImplicits._

import scala.concurrent.ExecutionContext

trait Shells[F[_]] {
  implicit protected def F: ConcurrentEffect[F]
  protected def orkestraConfig: OrkestraConfig
  protected def kubernetesClient: Resource[F, KubernetesClient[F]]

  private def runningMessage(script: String) = Sync[F].delay(println(s"Running: $script"))

  /**
    * Run a shell script in the work directory passed in the implicit workDir.
    */
  def sh(script: String)(implicit workDir: Directory): F[String] =
    runningMessage(script) *>
      Sync[F].delay(Process(Seq("sh", "-c", script), workDir.path.toFile).lineStream.fold("") { (acc, line) =>
        println(line)
        s"$acc\n$line"
      })

  /**
    * Run a shell script in the given container and in the work directory passed in the implicit workDir.
    */
  def sh(script: String, container: Container)(
    implicit workDir: Directory,
    timer: Timer[F]
  ): F[String] = {
    val sink = Sink.fold[String, Either[Status, String]]("") { (acc, data) =>
      data match {
        case Left(Status(_, _, _, _, _, _, _, Some("Success"))) =>
          println()
          acc
        case Left(Status(_, _, _, _, Some(message), _, Some(reason), _)) =>
          throw new IOException(s"$reason: $message; Container: ${container.name}; Script: $script")
        case Left(status) =>
          throw new IOException(
            s"Non success container termination: $status; Container: ${container.name}; Script: $script"
          )
        case Right(log) =>
          print(log)
          acc + log
      }
    }
    val flow = Flow.fromSinkAndSourceMat(sink, Source.maybe[Message])(Keep.left)

    def exec(timeout: Duration = 1.minute, interval: FiniteDuration = 300.millis): F[String] =
      kubernetesClient.use(
        _.pods
          .namespace(orkestraConfig.namespace)
          .exec(
            orkestraConfig.podName,
            flow,
            Option(container.name),
            Seq("sh", "-c", s"cd ${workDir.path.toAbsolutePath} && $script"),
            stdin = true,
            tty = true
          )
          .recoverWith {
            case _: KubernetesException if timeout > 0.milli =>
              timer.sleep(interval) *> exec(timeout - interval, interval)
          }
      )

    runningMessage(script) *> exec()
  }
}

object Shells extends Shells[IO] {
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  override lazy val F: ConcurrentEffect[IO] = IO.ioConcurrentEffect
  implicit override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  override lazy val kubernetesClient: Resource[IO, KubernetesClient[IO]] = Kubernetes.client
}
