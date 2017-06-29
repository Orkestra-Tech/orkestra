package com.goyeau.orchestra.github

import java.util.UUID

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.goyeau.orchestra.{Job, RunInfo}
import io.circe.Json
import shapeless.{::, HList, HNil}

case class BranchTrigger(repoName: String, branchRegex: String, job: Job.Runner[_, String :: HNil, _, _ <: HList]) {
  def trigger(eventType: String,
              json: Json)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Unit =
    eventType match {
      case "create" | "push" =>
        for {
          eventRepoNameJson <- json.hcursor.downField("repository").downField("full_name").focus
          eventRepoName = eventRepoNameJson.as[String].fold(throw _, identity)
          if eventRepoName == repoName
          eventBranchJson <- json.hcursor.downField("ref").focus
          eventBranch = eventBranchJson.as[String].fold(throw _, identity).replace("refs/heads/", "")
          if branchRegex.r.findFirstIn(eventBranch).isDefined
        } yield job.apiServer.run(RunInfo(job.definition.id, Option(UUID.randomUUID())), eventBranch :: HNil)
      case _ =>
    }
}
