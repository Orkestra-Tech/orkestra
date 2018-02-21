package io.chumps.orchestra.github

import java.io.{IOException, PrintWriter, StringWriter}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.util.ByteString
import com.typesafe.scalalogging.{LazyLogging, Logger}

import io.chumps.orchestra.{BuildInfo, OrchestraConfig, OrchestraPlugin}
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.github.State._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import io.chumps.orchestra.filesystem.{Directory, LocalFile}

trait Github extends OrchestraPlugin {
  private lazy val logger = Logger(getClass)

  def githubTriggers: Set[GithubTrigger]

  override def onMasterStart(): Future[Unit] =
    for {
      _ <- super.onMasterStart()
      _ = logger.info("Starting Github triggers webhook")

      routes = path("health") {
        complete(OK)
      } ~
        path("webhooks") {
          headerValueByName("X-GitHub-Event") { eventType =>
            entity(as[String]) { entity =>
              onSuccess(Future.traverse(githubTriggers)(_.trigger(eventType, parse(entity).fold(throw _, identity)))) {
                case triggers if triggers.contains(true) => complete(OK)
                case _                                   => complete(Accepted)

              }
            }
          }
        }

      _ = Http().bindAndHandle(routes, "0.0.0.0", GithubConfig.port)
    } yield ()
}

object Github extends LazyLogging {

  def pullRequest[Result](repository: Repository, ref: Branch) = PullRequestBuilder(repository, ref)

  case class PullRequestBuilder(repository: Repository, ref: Branch) {
    def apply[Result](body: Directory => Result)(implicit workDir: Directory): Result =
      Await.result(apply((workDir: Directory) => Future(body(workDir))), Duration.Inf)

    def apply[Result](body: Directory => Future[Result])(implicit workDir: Directory): Future[Result] =
      (for {
        _ <- pushStatus(repository, ref, State.Pending)
        _ <- cloneRepo(repository, ref)
        result <- body(Directory(LocalFile(repository.name)))
        _ <- pushStatus(repository, ref, State.Success)
      } yield result).recoverWith {
        case throwable: Throwable =>
          pushStatus(repository, ref, State.Failure)
          Future.failed(throwable)
      }
  }

  private def cloneRepo(repository: Repository, ref: Branch)(implicit workDir: Directory) = Future {
    val git = Git
      .cloneRepository()
      .setURI(s"https://github.com/${repository.name}.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(BuildInfo.projectName.toLowerCase, GithubConfig.token)
      )
      .setDirectory(LocalFile(repository.name))
      .setNoCheckout(true)
      .call()
    git.checkout().setName(ref.name).call()
  }

  private def pushStatus(repository: Repository, ref: Branch, state: State) =
    for {
      response <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"https://api.github.com/repos/${repository.name}/statuses/${ref.name}",
          List(Authorization(OAuth2BearerToken(GithubConfig.token))),
          HttpEntity(
            CheckStatus(
              state,
              s"${GithubConfig.url}/#/logs/${OrchestraConfig.runInfo.runId.value}",
              state.description,
              s"${BuildInfo.projectName.toLowerCase}/pull-request"
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
    } yield response
}
