package io.chumps.orchestra.github

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.Materializer

import io.chumps.orchestra.Job
import io.circe.Json
import shapeless.{::, HNil}

import io.chumps.orchestra.model.RunId

sealed trait GithubTrigger {
  private[github] def trigger(eventType: String,
                              json: Json)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Unit
}

case class BranchTrigger(repoName: String, branchRegex: String, job: Job.Runner[String :: HNil, _])
    extends GithubTrigger {
  private[github] def trigger(eventType: String, json: Json)(implicit ec: ExecutionContext,
                                                             system: ActorSystem,
                                                             mat: Materializer): Unit =
    eventType match {
      case "create" | "push" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val eventBranch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (eventRepoName == repoName && branchRegex.r.findFirstIn(eventBranch).isDefined)
          job.ApiServer.trigger(RunId.random(), eventBranch :: HNil)
      case _ =>
    }
}

case class PullRequestTrigger(repoName: String, job: Job.Runner[String :: HNil, _]) extends GithubTrigger {
  private[github] def trigger(eventType: String, json: Json)(implicit ec: ExecutionContext,
                                                             system: ActorSystem,
                                                             mat: Materializer): Unit =
    eventType match {
      case "pull_request" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val prBranch =
          json.hcursor.downField("pull_request").downField("head").downField("sha").as[String].fold(throw _, identity)

        if (eventRepoName == repoName) job.ApiServer.trigger(RunId.random(), prBranch :: HNil, Seq(prBranch))
      case _ =>
    }
}
