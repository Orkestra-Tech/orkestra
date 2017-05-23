package com.goyeau.orchestra.kubernetes

import java.io.{FileInputStream, FileOutputStream}
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
import com.goyeau.orchestra.{AutowireServer, RunInfo}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._

object Job {

  def schedule(jobId: Symbol,
               runInfo: RunInfo)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) = {
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
          entity = HttpEntity(ContentTypes.`application/json`, ByteString(createJob(masterPod, runInfo).noSpaces))
        )
      )
      jobScheduleEntity <- jobScheduleResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _)
    } yield println("entity2: " + jobScheduleEntity.utf8String)
  }

  private def authToken() = {
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

  private def createJob(masterPod: Json, runInfo: RunInfo) = {
    val spec = masterPod.hcursor.downField("spec")
    val container = spec.downField("containers").downArray.first
    val runInfoEnv = Json.obj(
      "name" -> Json.fromString("ORCHESTRA_RUN_INFO"),
      "value" -> Json.fromString(AutowireServer.write(runInfo))
    )
    val envs = container.downField("env").focus.get.asArray.get :+ runInfoEnv
    val name = s"orchestra-slave-${runInfo.id}"

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
                "volumeMounts" -> container.downField("volumeMounts").focus.get
              )
            ),
            "volumes" -> spec.downField("volumes").focus.get,
            "restartPolicy" -> Json.fromString("Never")
          )
        )
      )
    )
  }
}
