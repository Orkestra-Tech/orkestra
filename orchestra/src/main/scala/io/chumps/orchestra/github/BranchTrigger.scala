package io.chumps.orchestra.github

import io.circe.Json
import shapeless._

import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.utils.RunIdOperation

case class Branch(name: String)

sealed trait GithubTrigger {
  private[github] def trigger(eventType: String, json: Json): Unit
}

case class BranchTrigger[ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList, ParamValues <: HList] private (
  repoName: String,
  branchRegex: String,
  job: JobRunner[ParamValues, _],
  values: ParamValuesNoRunIdBranch
)(
  implicit runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch],
  branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues]
) extends GithubTrigger {
  private[github] def trigger(eventType: String, json: Json): Unit =
    eventType match {
      case "create" | "push" =>
        val repoName = json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (repoName == repoName && branchRegex.r.findFirstIn(branch).isDefined) {
          val runId = RunId.random()
          job.ApiServer
            .trigger(runId, branchInjector(runIdOperation.inject(values, runId), Branch(branch)))
        }
      case _ =>
    }
}

object BranchTrigger {

  def apply[ParamValues <: HList](repoName: String, branchRegex: String, job: JobRunner[ParamValues, _]) =
    new BranchTriggerBuilder[ParamValues](repoName, branchRegex, job)

  class BranchTriggerBuilder[ParamValues <: HList](repoName: String,
                                                   branchRegex: String,
                                                   job: JobRunner[ParamValues, _]) {
    // No Params
    def apply[ParamValuesNoBranch <: HList]()(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[HNil, ParamValuesNoBranch]
    ): BranchTrigger[HNil, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repoName, branchRegex, job, HNil)

    // One param
    def apply[ParamValuesNoBranch <: HList, ParamValueNoRunIdBranch](value: ParamValueNoRunIdBranch)(
      implicit branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch]
    ): BranchTrigger[ParamValueNoRunIdBranch :: HNil, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repoName, branchRegex, job, value :: HNil)

    // Multi param
    def apply[TupledValues <: Product, ParamValuesNoRunIdBranch <: HList, ParamValuesNoBranch <: HList](
      paramValues: TupledValues
    )(
      implicit tupleToHList: Generic.Aux[TupledValues, ParamValuesNoRunIdBranch],
      branchInjector: BranchInjector[ParamValuesNoBranch, ParamValues],
      runIdOperation: RunIdOperation[ParamValuesNoRunIdBranch, ParamValuesNoBranch]
    ): BranchTrigger[ParamValuesNoRunIdBranch, ParamValuesNoBranch, ParamValues] =
      BranchTrigger(repoName, branchRegex, job, tupleToHList.to(paramValues))

  }
}

case class PullRequestTrigger(repoName: String, job: JobRunner[String :: HNil, _]) extends GithubTrigger {
  private[github] def trigger(eventType: String, json: Json): Unit =
    eventType match {
      case "pull_request" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val branch =
          json.hcursor.downField("pull_request").downField("head").downField("sha").as[String].fold(throw _, identity)

        if (eventRepoName == repoName) job.ApiServer.trigger(RunId.random(), branch :: HNil, Seq(branch))
      case _ =>
    }
}
