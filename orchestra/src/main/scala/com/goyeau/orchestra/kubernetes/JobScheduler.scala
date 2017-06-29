package com.goyeau.orchestra.kubernetes

import java.io.IOException

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

object JobScheduler extends LazyLogging {

  def apply[Containers <: HList](
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
            ByteString(createJob(masterPod, runInfo, podConfig).noSpaces)
          )
        )
      )
      jobScheduleEntity <- jobScheduleResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (jobScheduleResponse.status.isFailure()) {
        val message = s"Scheduling job '${runInfo.jobId}' on Kubernetes failed: ${jobScheduleEntity.utf8String}"
        logger.error(message)
        throw new IOException(message)
      }

  private def createJob[Containers <: HList](masterPod: Json, runInfo: RunInfo, podConfig: PodConfig[Containers]) =
    Json.obj(
      "apiVersion" -> Json.fromString("batch/v1"),
      "kind" -> Json.fromString("Job"),
      "metadata" -> Json.obj("name" -> Json.fromString(s"orchestra-slave-${runInfo.runId}")),
      "spec" -> JobUtils.createSpec(masterPod, runInfo, podConfig)
    )
}
