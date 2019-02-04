package tech.orkestra.github

import scala.concurrent.Future
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.http.ElasticClient
import io.circe.Json
import shapeless._
import tech.orkestra.OrkestraConfig
import tech.orkestra.job.Job
import tech.orkestra.model.RunId
import tech.orkestra.utils.AkkaImplicits._

sealed trait GithubTrigger[F[_]] {
  private[github] def trigger(eventType: String, json: Json)(
    implicit
    orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ): Future[Boolean]
}

case class BranchTrigger[F[_], ParametersNoGitRef <: HList, Parameters <: HList](
  repository: Repository,
  branchRegex: String,
  job: Job[F, Parameters, _],
  parameters: ParametersNoGitRef
)(implicit gitRefInjector: GitRefInjector[ParametersNoGitRef, Parameters])
    extends GithubTrigger[F] {

  private[github] def trigger(eventType: String, json: Json)(
    implicit
    orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ): Future[Boolean] =
    eventType match {
      case "push" =>
        val repoName = json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (repoName == repository.name && s"^$branchRegex$$".r.findFirstIn(branch).isDefined) {
          val runId = RunId.random()
          job
            .ApiServer()(orkestraConfig, kubernetesClient, elasticsearchClient)
            .trigger(runId, gitRefInjector(parameters, GitRef(branch)))
            .map(_ => true)
        } else Future.successful(false)
      case _ => Future.successful(false)
    }
}

case class PullRequestTrigger[F[_], ParametersNoGitRef <: HList, Parameters <: HList](
  repository: Repository,
  job: Job[F, Parameters, _],
  parameters: ParametersNoGitRef
)(implicit gitRefInjector: GitRefInjector[ParametersNoGitRef, Parameters])
    extends GithubTrigger[F] {

  private[github] def trigger(eventType: String, json: Json)(
    implicit
    orkestraConfig: OrkestraConfig,
    kubernetesClient: KubernetesClient[F],
    elasticsearchClient: ElasticClient
  ): Future[Boolean] =
    eventType match {
      case "pull_request" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch =
          json.hcursor.downField("pull_request").downField("head").downField("sha").as[String].fold(throw _, identity)

        if (eventRepoName == repository.name) {
          val runId = RunId.random()
          job
            .ApiServer()
            .trigger(runId, gitRefInjector(parameters, GitRef(branch)), Seq(branch))
            .map(_ => true)
        } else Future.successful(false)
      case _ => Future.successful(false)
    }
}
