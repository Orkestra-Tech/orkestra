package com.goyeau.orkestra.github

import scala.concurrent.Future

import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.Json
import shapeless._

import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.job.Job
import com.goyeau.orkestra.kubernetes.Kubernetes
import com.goyeau.orkestra.model.RunId
import com.goyeau.orkestra.utils.{Elasticsearch, RunIdOperation}
import com.goyeau.orkestra.utils.AkkaImplicits._

sealed trait GithubTrigger {
  private[github] def trigger(eventType: String, json: Json): Future[Boolean]
}

case class BranchTrigger[ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList, ParamValues <: HList] private (
  repository: Repository,
  branchRegex: String,
  job: Job[ParamValues, _],
  values: ParamValuesNoRunIdBranch
)(
  implicit runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch],
  gitRefInjector: GitRefInjector[ParamValuesNoBranch, ParamValues],
  orkestraConfig: OrkestraConfig,
  kubernetesClient: KubernetesClient,
  elasticsearchClient: HttpClient
) extends GithubTrigger {

  private[github] def trigger(eventType: String, json: Json): Future[Boolean] =
    eventType match {
      case "push" =>
        val repoName = json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (repoName == repository.name && s"^$branchRegex$$".r.findFirstIn(branch).isDefined) {
          val runId = RunId.random()
          job
            .ApiServer()
            .trigger(runId, gitRefInjector(runIdOperation.inject(values, runId), GitRef(branch)))
            .map(_ => true)
        } else Future.successful(false)
      case _ => Future.successful(false)
    }
}

object BranchTrigger {
  implicit private lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  implicit private lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  implicit private lazy val httpClient: HttpClient = Elasticsearch.client

  def apply[ParamValues <: HList](repository: Repository, branchRegex: String, job: Job[ParamValues, _]) =
    new BranchTriggerBuilder[ParamValues](repository, branchRegex, job)

  class BranchTriggerBuilder[ParamValues <: HList](
    repository: Repository,
    branchRegex: String,
    job: Job[ParamValues, _]
  ) {

    // No Params
    def apply[ParamValuesNoGitRef <: HList]()(
      implicit gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[HNil, ParamValuesNoGitRef]
    ): BranchTrigger[HNil, ParamValuesNoGitRef, ParamValues] =
      BranchTrigger(repository, branchRegex, job, HNil)

    // One param
    def apply[ParamValuesNoGitRef <: HList, ParamValueNoRunIdGitRef](value: ParamValueNoRunIdGitRef)(
      implicit gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[ParamValueNoRunIdGitRef :: HNil, ParamValuesNoGitRef]
    ): BranchTrigger[ParamValueNoRunIdGitRef :: HNil, ParamValuesNoGitRef, ParamValues] =
      BranchTrigger(repository, branchRegex, job, value :: HNil)

    // Multi param
    def apply[TupledValues <: Product, ParamValuesNoRunIdGitRef <: HList, ParamValuesNoGitRef <: HList](
      paramValues: TupledValues
    )(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunIdGitRef],
      gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[ParamValuesNoRunIdGitRef, ParamValuesNoGitRef]
    ): BranchTrigger[ParamValuesNoRunIdGitRef, ParamValuesNoGitRef, ParamValues] =
      BranchTrigger(repository, branchRegex, job, tupleToHList.to(paramValues))
  }
}

case class PullRequestTrigger[ParamValuesNoRunIdGitRef <: HList, ParamValuesNoGitRef <: HList, ParamValues <: HList] private (
  repository: Repository,
  job: Job[ParamValues, _],
  values: ParamValuesNoRunIdGitRef
)(
  implicit runIdOperation: RunIdOperation[ParamValuesNoRunIdGitRef, ParamValuesNoGitRef],
  gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
  orkestraConfig: OrkestraConfig,
  kubernetesClient: KubernetesClient,
  elasticsearchClient: HttpClient
) extends GithubTrigger {

  private[github] def trigger(eventType: String, json: Json): Future[Boolean] =
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
            .trigger(runId, gitRefInjector(runIdOperation.inject(values, runId), GitRef(branch)), Seq(branch))
            .map(_ => true)
        } else Future.successful(false)
      case _ => Future.successful(false)
    }
}

object PullRequestTrigger {
  implicit private lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
  implicit private lazy val kubernetesClient: KubernetesClient = Kubernetes.client
  implicit private lazy val httpClient: HttpClient = Elasticsearch.client

  def apply[ParamValues <: HList](repository: Repository, job: Job[ParamValues, _]) =
    new PullRequestTriggerBuilder[ParamValues](repository, job)

  class PullRequestTriggerBuilder[ParamValues <: HList](repository: Repository, job: Job[ParamValues, _]) {

    // No Params
    def apply[ParamValuesNoGitRef <: HList]()(
      implicit gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[HNil, ParamValuesNoGitRef]
    ): PullRequestTrigger[HNil, ParamValuesNoGitRef, ParamValues] =
      PullRequestTrigger(repository, job, HNil)

    // One param
    def apply[ParamValuesNoGitRef <: HList, ParamValueNoRunIdGitRef](value: ParamValueNoRunIdGitRef)(
      implicit gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[ParamValueNoRunIdGitRef :: HNil, ParamValuesNoGitRef]
    ): PullRequestTrigger[ParamValueNoRunIdGitRef :: HNil, ParamValuesNoGitRef, ParamValues] =
      PullRequestTrigger(repository, job, value :: HNil)

    // Multi param
    def apply[TupledValues <: Product, ParamValuesNoRunIdGitRef <: HList, ParamValuesNoGitRef <: HList](
      paramValues: TupledValues
    )(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunIdGitRef],
      gitRefInjector: GitRefInjector[ParamValuesNoGitRef, ParamValues],
      runIdOperation: RunIdOperation[ParamValuesNoRunIdGitRef, ParamValuesNoGitRef]
    ): PullRequestTrigger[ParamValuesNoRunIdGitRef, ParamValuesNoGitRef, ParamValues] =
      PullRequestTrigger(repository, job, tupleToHList.to(paramValues))
  }
}
