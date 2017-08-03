//package com.goyeau.orchestra.kubernetes
//
//import java.io.IOException
//
//import scala.concurrent.ExecutionContext
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
//import akka.stream.Materializer
//import akka.util.ByteString
//import com.goyeau.orchestra.cron.CronTrigger
//import com.goyeau.orchestra.{Config, RunInfo}
//import com.typesafe.scalalogging.LazyLogging
//import io.circe.Json
//import io.fabric8.kubernetes.api.model.Pod
//
//object CronJob extends LazyLogging {
//
//  def create(
//    cronTrigger: CronTrigger
//  )(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) =
//    for {
//      masterPod <- MasterPod.get()
//      cronJobScheduleResponse <- Http().singleRequest(
//        HttpRequest(
//          HttpMethods.POST,
//          s"${Config.kubeUri}/apis/batch/v2alpha1/namespaces/${Config.namespace}/cronjobs",
//          headers = List(Auth.header),
//          entity = HttpEntity(
//            ContentTypes.`application/json`,
//            ByteString(createCronJob(masterPod, cronTrigger).noSpaces)
//          )
//        )
//      )
//      jobScheduleEntity <- cronJobScheduleResponse.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
//    } yield
//      if (cronJobScheduleResponse.status.isFailure) {
//        val message =
//          s"Scheduling Kubernetes cron job '${cronTrigger.job.definition.id}' failed: ${jobScheduleEntity.utf8String}"
//        logger.error(message)
//        throw new IOException(message)
//      }
//
//  private def createCronJob(masterPod: Pod, cronTrigger: CronTrigger) = {
//    val runInfo = RunInfo(cronTrigger.job.definition.id, None)
//
//    Json.obj(
//      "apiVersion" -> Json.fromString("batch/v2alpha1"),
//      "kind" -> Json.fromString("CronJob"),
//      "metadata" -> Json.obj("name" -> Json.fromString(s"orchestra-cronjob-${runInfo.jobId.name.toLowerCase}")),
//      "spec" -> Json.obj(
//        "schedule" -> Json.fromString(cronTrigger.schedule),
//        "jobTemplate" -> Json.obj(
//          "spec" -> ContainerUtils.createSpec(masterPod, runInfo, cronTrigger.job.podConfig)
//        )
//      )
//    )
//  }
//}
