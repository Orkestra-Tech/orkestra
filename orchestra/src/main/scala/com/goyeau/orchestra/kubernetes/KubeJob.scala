package com.goyeau.orchestra.kubernetes

import java.io.{File, FileInputStream, FileOutputStream}
import java.security.KeyStore
import java.security.cert.CertificateFactory

import scala.concurrent.ExecutionContext
import scala.io.Source

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import com.goyeau.orchestra.{AutowireServer, OrchestraConfig, RunInfo}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import shapeless.HList

object KubeJob {

  def schedule[Containers <: HList](
    jobId: Symbol,
    runInfo: RunInfo,
    podConfig: PodConfig[Containers]
  )(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) = {
    val token = Authorization(OAuth2BearerToken(authToken()))

    for {
      masterPodResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.GET,
          s"${KubeConfig.uri}/api/v1/namespaces/${KubeConfig.namespace}/pods/${KubeConfig.podName}",
          headers = List(token)
        )
      )
      masterPodEntity <- masterPodResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
      masterPod = parse(masterPodEntity.utf8String).fold(throw _, identity)

      jobScheduleResponse <- Http().singleRequest(
        HttpRequest(
          HttpMethods.POST,
          s"${KubeConfig.uri}/apis/batch/v1/namespaces/${KubeConfig.namespace}/jobs",
          headers = List(token),
          entity = HttpEntity(
            ContentTypes.`application/json`,
            ByteString(createJob(masterPod, runInfo, podConfig).noSpaces)
          )
        )
      )
      jobScheduleEntity <- jobScheduleResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield
      if (jobScheduleResponse.status.isFailure())
        throw new IllegalStateException(s"Scheduling job on Kubernetes failed: ${jobScheduleEntity.utf8String}")
  }

  def authToken() = {
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null, Array.empty)

    val certificateFactory = CertificateFactory.getInstance("X.509")
    val cert = certificateFactory
      .generateCertificate(new FileInputStream("/var/run/secrets/kubernetes.io/serviceaccount/ca.crt"))
    keyStore.setCertificateEntry("kube", cert)

    keyStore.store(new FileOutputStream("cacerts"), Array.empty)
    System.setProperty("javax.net.ssl.trustStore", "cacerts")
    System.setProperty("javax.net.ssl.keyStore", "cacerts")

    Source.fromFile("/var/run/secrets/kubernetes.io/serviceaccount/token").mkString
  }

  private def createJob[Containers <: HList](masterPod: Json, runInfo: RunInfo, podConfig: PodConfig[Containers]) = {
    val spec = masterPod.hcursor.downField("spec")
    val container = spec.downField("containers").downArray.first
    val runInfoEnv = Json.obj(
      "name" -> Json.fromString("ORCHESTRA_RUN_INFO"),
      "value" -> Json.fromString(AutowireServer.write(runInfo))
    )
    val envs = container.downField("env").focus.get.asArray.get :+ runInfoEnv
    val name = s"orchestra-slave-${runInfo.id}"
    val workspace = s"/opt/docker/${OrchestraConfig.workspace.getPath}"
    val homeDirMount = Json.obj(
      "mountPath" -> Json.fromString(workspace),
      "name" -> Json.fromString("home")
    )
    val homeDirVolume = Json.obj(
      "name" -> Json.fromString("home"),
      "emptyDir" -> Json.obj()
    )
    val containers = podConfig.containerSeq.map { container =>
      Json.obj(
        "name" -> Json.fromString(container.name),
        "image" -> Json.fromString(container.image),
        "stdin" -> Json.True,
        "stdout" -> Json.True,
        "stderr" -> Json.True,
        "tty" -> Json.fromBoolean(container.tty),
        "command" -> Json.fromValues(container.command.map(Json.fromString)),
        "workingDir" -> Json.fromString(workspace),
        "volumeMounts" -> Json.arr(homeDirMount)
      )
    }

    Json.obj(
      "apiVersion" -> Json.fromString("batch/v1"),
      "kind" -> Json.fromString("Job"),
      "metadata" -> Json.obj("name" -> Json.fromString(name)),
      "spec" -> Json.obj(
        "template" -> Json.obj(
          "metadata" -> Json.obj("name" -> Json.fromString(name)),
          "spec" -> Json.obj(
            "containers" -> Json.arr(
              Json.obj(
                "name" -> container.downField("name").focus.get,
                "image" -> container.downField("image").focus.get,
                "env" -> Json.fromValues(envs),
                "volumeMounts" -> Json.arr(
                  container.downField("volumeMounts").values.toVector.flatten :+ homeDirMount: _*
                )
              ) +: containers: _*
            ),
            "volumes" -> Json.arr(spec.downField("volumes").values.toVector.flatten :+ homeDirVolume: _*),
            "restartPolicy" -> Json.fromString("Never")
          )
        )
      )
    )
  }
}
