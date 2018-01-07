package io.chumps.orchestra.github

import java.io.{IOException, PrintWriter, StringWriter}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging

import io.chumps.orchestra.{BuildInfo, JVMApp, OrchestraConfig}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.github.State._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import io.chumps.orchestra.filesystem.{Directory, LocalFile}

trait Github extends JVMApp {

  def githubTriggers: Set[GithubTrigger]

  override def main(args: Array[String]): Unit = {
    super.main(args)

    lazy val routes =
      path("health") {
        complete(OK)
      } ~
        path("webhooks") {
          headerValueByName("X-GitHub-Event") { eventType =>
            entity(as[String]) { entity =>
              val json = parse(entity).fold(throw _, identity)
              githubTriggers.foreach(_.trigger(eventType, json))
              complete(OK)
            }
          }
        }

    if (OrchestraConfig.runInfoMaybe.isEmpty) Http().bindAndHandle(routes, "0.0.0.0", OrchestraConfig.githubPort)
  }
}

object Github extends LazyLogging {

  def pullRequest[T](repository: Repository, ref: Branch)(body: Directory => T)(implicit workDir: Directory): T =
    try {
      notify(repository, ref, State.Pending)
      clone(repository, ref)
      val result = body(Directory(LocalFile(repository.name)))
      notify(repository, ref, State.Success)
      result
    } catch {
      case t: Throwable =>
        notify(repository, ref, State.Failure)
        throw t
    }

  private def clone(repository: Repository, ref: Branch)(implicit workDir: Directory): Unit = {
    val git = Git
      .cloneRepository()
      .setURI(s"https://github.com/${repository.name}.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(OrchestraConfig.appName.toLowerCase, OrchestraConfig.githubToken)
      )
      .setDirectory(LocalFile(repository.name))
      .setNoCheckout(true)
      .call()
    git.checkout().setName(ref.name).call()
  }

  private def notify(repository: Repository, ref: Branch, state: State) = Await.result(
    for {
      response <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"https://api.github.com/repos/${repository.name}/statuses/${ref.name}",
          List(Authorization(OAuth2BearerToken(OrchestraConfig.githubToken))),
          HttpEntity(
            CheckStatus(
              state,
              s"${OrchestraConfig.url}/#/logs/${OrchestraConfig.runInfo.runId.value}",
              state.description,
              s"${OrchestraConfig.appName.toLowerCase}/pull-request"
            ).asJson.noSpaces
          )
        )
      )
      entity <- response.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
      _ = if (response.status.isFailure) {
        val exception = new IOException(s"${response.status.reason}: ${entity.utf8String}")
        val writer = new StringWriter()
        exception.printStackTrace(new PrintWriter(writer, true))
        logger.error(writer.toString)
        throw exception
      }
    } yield response,
    Duration.Inf
  )
}
