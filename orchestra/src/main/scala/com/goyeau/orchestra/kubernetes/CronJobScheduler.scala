package com.goyeau.orchestra.kubernetes

import java.io.IOException

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import com.goyeau.orchestra.cron.CronTrigger
import com.goyeau.orchestra.RunInfo
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json

object CronJobScheduler extends LazyLogging {

  def apply(
    cronTrigger: CronTrigger
  )(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
    for {
      masterPod <- MasterPod.get()
      cronJobScheduleResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"${KubeConfig.uri}/apis/batch/v2alpha1/namespaces/${KubeConfig.namespace}/cronjobs",
          headers = List(Auth.header),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            ByteString(createCronJob(masterPod, cronTrigger).noSpaces)
          )
        )
      )
      jobScheduleEntity <- cronJobScheduleResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (cronJobScheduleResponse.status.isFailure) {
        val message =
          s"Scheduling cron job '${cronTrigger.job.definition.id}' on Kubernetes failed: ${jobScheduleEntity.utf8String}"
        logger.error(message)
        throw new IOException(message)
      }

  private def createCronJob(masterPod: Json, cronTrigger: CronTrigger) = {
    val runInfo = RunInfo(cronTrigger.job.definition.id, None)

    Json.obj(
      "apiVersion" -> Json.fromString("batch/v2alpha1"),
      "kind" -> Json.fromString("CronJob"),
      "metadata" -> Json.obj("name" -> Json.fromString(s"orchestra-cronjob-${runInfo.jobId.name.toLowerCase}")),
      "spec" -> Json.obj(
        "schedule" -> Json.fromString(cronTrigger.schedule),
        "jobTemplate" -> Json.obj(
          "spec" -> JobUtils.createSpec(masterPod, runInfo, cronTrigger.job.podConfig)
        )
      )
    )
  }
}
