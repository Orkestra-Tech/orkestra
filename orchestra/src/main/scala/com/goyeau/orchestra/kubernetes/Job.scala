package com.goyeau.orchestra.kubernetes

import java.io.IOException
import java.util.UUID

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import com.goyeau.orchestra.RunInfo
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import shapeless.HList

object Job extends LazyLogging {

  private def jobName(runId: UUID) = s"orchestra-slave-$runId"

  def create[Containers <: HList](
    runInfo: RunInfo,
    podConfig: PodConfig[Containers]
  )(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      masterPod <- MasterPod.get()
      jobScheduleResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"${KubeConfig.uri}/apis/batch/v1/namespaces/${KubeConfig.namespace}/jobs",
          headers = List(Auth.header),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            ByteString(jobJson(masterPod, runInfo, podConfig).noSpaces)
          )
        )
      )
      jobScheduleEntity <- jobScheduleResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (jobScheduleResponse.status.isFailure()) {
        val message = s"Scheduling Kubernetes job '${runInfo.jobId}' failed: ${jobScheduleEntity.utf8String}"
        logger.error(message)
        throw new IOException(message)
      }

  private def jobJson[Containers <: HList](masterPod: Json, runInfo: RunInfo, podConfig: PodConfig[Containers]) =
    Json.obj(
      "apiVersion" -> Json.fromString("batch/v1"),
      "kind" -> Json.fromString("Job"),
      "metadata" -> Json.obj("name" -> Json.fromString(jobName(runInfo.runId))),
      "spec" -> ContainerUtils.createSpec(masterPod, runInfo, podConfig)
    )

  def delete(runInfo: RunInfo)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      _ <- deleteJob(jobName(runInfo.runId))
      _ <- Pod.delete(KubeConfig.podName)
    } yield ()

  private def deleteJob(jobName: String)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      podDeleteResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.DELETE,
          s"${KubeConfig.uri}/apis/batch/v1/namespaces/${KubeConfig.namespace}/jobs/$jobName",
          headers = List(Auth.header),
          entity = HttpEntity(ContentTypes.`application/json`, ByteString.empty)
        )
      )
      podDeleteEntity <- podDeleteResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (podDeleteResponse.status.isFailure()) {
        val message = s"Deleting Kubernetes job '$jobName' failed: ${podDeleteEntity.utf8String}"
        logger.error(message)
        throw new IOException(message)
      }
}
