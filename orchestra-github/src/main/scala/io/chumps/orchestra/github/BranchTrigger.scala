package io.chumps.orchestra.github

import io.circe.Json
import shapeless._

import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.utils.RunIdOperation

sealed trait GithubTrigger {
  private[github] def trigger(eventType: String, json: Json): Boolean
}

case class BranchTrigger[ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList, ParamValues <: HList] private (
  repository: Repository,
  branchRegex: String,
  job: JobRunner[ParamValues, _],
  values: ParamValuesNoRunIdBranch
)(
  implicit runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch],
  branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues]
) extends GithubTrigger {

  private[github] def trigger(eventType: String, json: Json): Boolean =
    eventType match {
      case "push" =>
        val repoName = json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (repoName == repository.name && s"^$branchRegex$$".r.findFirstIn(branch).isDefined) {
          val runId = RunId.random()
          job.ApiServer.trigger(runId, branchInjector(runIdOperation.inject(values, runId), Branch(branch)))
          true
        } else false
      case _ => false
    }
}

object BranchTrigger {

  def apply[ParamValues <: HList](repository: Repository, branchRegex: String, job: JobRunner[ParamValues, _]) =
    new BranchTriggerBuilder[ParamValues](repository, branchRegex, job)

  class BranchTriggerBuilder[ParamValues <: HList](repository: Repository,
                                                   branchRegex: String,
                                                   job: JobRunner[ParamValues, _]) {

    // No Params
    def apply[ParamValuesNoBranch <: HList]()(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[HNil, ParamValuesNoBranch]
    ): BranchTrigger[HNil, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repository, branchRegex, job, HNil)

    // One param
    def apply[ParamValuesNoBranch <: HList, ParamValueNoRunIdBranch](value: ParamValueNoRunIdBranch)(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch]
    ): BranchTrigger[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repository, branchRegex, job, value :: HNil)

    // Multi param
    def apply[TupledValues <: Product, ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList](
      paramValues: TupledValues
    )(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunIdBranch],
      branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch]
    ): BranchTrigger[ParamValuesNoRunIdBranch, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repository, branchRegex, job, tupleToHList.to(paramValues))

  }
}

case class PullRequestTrigger[ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList, ParamValues <: HList] private (
  repository: Repository,
  job: JobRunner[ParamValues, _],
  values: ParamValuesNoRunIdBranch
)(
  implicit runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch],
  branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues]
) extends GithubTrigger {

  private[github] def trigger(eventType: String, json: Json): Boolean =
    eventType match {
      case "pull_request" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch =
          json.hcursor.downField("pull_request").downField("head").downField("sha").as[String].fold(throw _, identity)

        if (eventRepoName == repository.name) {
          val runId = RunId.random()
          job.ApiServer.trigger(runId,
                                branchInjector(runIdOperation.inject(values, runId), Branch(branch)),
                                Seq(branch))
          true
        } else false
      case _ => false
    }
}

object PullRequestTrigger {

  def apply[ParamValues <: HList](repository: Repository, job: JobRunner[ParamValues, _]) =
    new PullRequestTriggerBuilder[ParamValues](repository, job)

  class PullRequestTriggerBuilder[ParamValues <: HList](repository: Repository, job: JobRunner[ParamValues, _]) {

    // No Params
    def apply[ParamValuesNoBranch <: HList]()(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[HNil, ParamValuesNoBranch]
    ): PullRequestTrigger[HNil, ParamValuesNoBranch, ParamValues] =
      PullRequestTrigger(repository, job, HNil)

    // One param
    def apply[ParamValuesNoBranch <: HList, ParamValueNoRunIdBranch](value: ParamValueNoRunIdBranch)(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch]
    ): PullRequestTrigger[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch, ParamValues] =
      PullRequestTrigger(repository, job, value :: HNil)

    // Multi param
    def apply[TupledValues <: Product, ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList](
      paramValues: TupledValues
    )(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunIdBranch],
      branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch]
    ): PullRequestTrigger[ParamValuesNoRunIdBranch, ParamValuesNoBranch, ParamValues] =
      PullRequestTrigger(repository, job, tupleToHList.to(paramValues))

  }
}
