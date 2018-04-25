package com.drivetribe.orchestra.github

import java.io.{IOException, PrintWriter, StringWriter}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.util.ByteString
import com.drivetribe.orchestra.{BuildInfo, OrchestraConfig}
import com.drivetribe.orchestra.filesystem.{Directory, LocalFile}
import com.drivetribe.orchestra.utils.AkkaImplicits._
import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import io.circe.generic.auto._
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

trait Github extends LazyLogging {
  protected val orchestraConfig: OrchestraConfig
  protected val githubConfig: GithubConfig

  /**
    * Clone the given repository, checkout the given Git ref and run a function updating the status for the Git ref.
    *
    * @param repository The Github repository to clone
    * @param ref The Git reference to checkout
    */
  def statusUpdated[Result](repository: Repository, ref: GitRef) = PullRequestBuilder(repository, ref)

  case class PullRequestBuilder(repository: Repository, ref: GitRef) {
    def apply[Result](func: Directory => Result)(implicit workDir: Directory): Result =
      Await.result(apply((workDir: Directory) => Future(func(workDir))), Duration.Inf)

    def apply[Result](func: Directory => Future[Result])(implicit workDir: Directory): Future[Result] =
      for {
        _ <- pushStatus(repository, ref, State.Pending)
        _ <- cloneRepo(repository, ref)
        result <- func(Directory(repository.name)).transformWith {
          case Success(result) =>
            println("Notifying Github check succeeded")
            pushStatus(repository, ref, State.Success).map(_ => result)
          case Failure(throwable: Throwable) =>
            println("Notifying Github check failed")
            pushStatus(repository, ref, State.Failure).flatMap(_ => Future.failed(throwable))
        }
      } yield result
  }

  private def cloneRepo(repository: Repository, ref: GitRef)(implicit workDir: Directory) = Future {
    val git = Git
      .cloneRepository()
      .setURI(s"https://github.com/${repository.name}.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(BuildInfo.projectName.toLowerCase, githubConfig.token)
      )
      .setDirectory(LocalFile(repository.name))
      .setNoCheckout(true)
      .call()
    git.checkout().setName(ref.name).call()
  }

  private def pushStatus(repository: Repository, ref: GitRef, state: State) =
    for {
      response <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"https://api.github.com/repos/${repository.name}/statuses/${ref.name}",
          List(Authorization(OAuth2BearerToken(githubConfig.token))),
          HttpEntity(
            CheckStatus(
              state,
              s"${githubConfig.uri}/#/logs/${orchestraConfig.runInfo.runId.value}",
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

object Github extends Github {
  implicit override val orchestraConfig: OrchestraConfig = OrchestraConfig.fromEnvVars()
  implicit override val githubConfig: GithubConfig = GithubConfig.fromEnvVars()
}
