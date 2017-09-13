package com.goyeau.orchestra.github

import java.util.UUID

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.goyeau.orchestra.{Job, RunInfo}
import io.circe.Json
import shapeless.{::, HList, HNil}

case class BranchTrigger(repoName: String, branchRegex: String, job: Job.Runner[String :: HNil, _, _ <: HList]) {
  def trigger(eventType: String,
              json: Json)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Unit =
    eventType match {
      case "create" | "push" =>
        val eventRepoName =
          json.hcursor.downField("repository").downField("full_name").as[String].fold(throw _, identity)
        val eventBranch = json.hcursor.downField("ref").as[String].fold(throw _, identity).replace("refs/heads/", "")

        if (eventRepoName == repoName && branchRegex.r.findFirstIn(eventBranch).isDefined)
          job.apiServer.trigger(RunInfo(job.definition.id, Option(UUID.randomUUID())), eventBranch :: HNil)
      case _ =>
    }
}
