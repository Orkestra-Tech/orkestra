package com.drivetribe.orchestration.frontend

import java.io.File
import java.time.ZonedDateTime
import java.util.Date

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.{ObjectMetadataProvider, TransferManagerBuilder}
import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Environment, EnvironmentType, Lock, Project}
import com.goyeau.kubernetesclient.{KubeConfig, KubernetesClient}
import com.goyeau.orchestra.filesystem.LocalFile
import com.goyeau.orchestra.{Job, _}
import com.goyeau.orchestra.AkkaImplicits._
import com.typesafe.scalalogging.{LazyLogging, Logger}
import io.k8s.api.apps.v1beta1.{Deployment, DeploymentSpec, DeploymentStrategy, RollingUpdateDeployment}
import io.k8s.api.core.v1._
import io.k8s.apimachinery.pkg.api.resource.Quantity
import io.k8s.apimachinery.pkg.apis.meta.v1.ObjectMeta
import io.k8s.apimachinery.pkg.util.intstr.IntOrString

object DeployFrontend {

  def jobDefinition(environment: Environment) = Job[String => Unit](Symbol(s"deployFrontend$environment"))

  def job(environment: Environment) = jobDefinition(environment)(apply(environment) _)

  def board(environment: Environment) =
    SingleJobBoard("Deploy Frontend", jobDefinition(environment))(Param[String]("Version"))

  lazy val logger = Logger(getClass)

  def apply(environment: Environment)(version: String): Unit =
    Lock.onDeployment(environment, Project.Frontend) {
      webFrontend(version)
      webBackend(version, environment)
    }

  def webFrontend(version: String) = {
    logger.info("Deploy web frontend")

    val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.EU_WEST_1).build
    val transferManager = TransferManagerBuilder.standard.withS3Client(s3).build
    val packageName = s"web-frontend-$version.zip"
    transferManager.download("drivetribe-web-releases", packageName, LocalFile(s"./$packageName")).waitForCompletion()

    sh(s"""mkdir temp
          |unzip web-frontend-$version.zip -d temp
          |""".stripMargin)

    val metadataProvider = new ObjectMetadataProvider {
      override def provideObjectMetadata(file: File, metadata: ObjectMetadata): Unit = {
        metadata.setExpirationTime(Date.from(ZonedDateTime.now().plusYears(1).toInstant))
        metadata.setCacheControl("max-age=31536000")
      }
    }

    transferManager.uploadDirectory(
      "drivetribe-live-frontend-application",
      version,
      LocalFile("temp"),
      true,
      metadataProvider
    )
  }

  def webBackend(version: String, environment: Environment) = {
    logger.info("Deploy web backend")
    if (environment.environmentType == EnvironmentType.Medium) deployOnKubernetes(version, environment)
    else deployOnBeanstalk(version, environment)
  }

  def deployOnKubernetes(version: String, environment: Environment) = {
    val kube = KubernetesClient(KubeConfig(new File("/opt/docker/secrets/kube/config")))

    val deployment = Deployment(
      metadata = Option(ObjectMeta(name = Option("web-backend"), namespace = Option(environment.entryName))),
      spec = Option(
        DeploymentSpec(
          replicas = Option(2),
          strategy = Option(
            DeploymentStrategy(
              `type` = Option("RollingUpdate"),
              rollingUpdate = Option(RollingUpdateDeployment(Option(IntOrString("10%")), Option(IntOrString("50%"))))
            )
          ),
          template = PodTemplateSpec(
            metadata = Option(
              ObjectMeta(
                labels = Option(Map("app" -> "web", "tier" -> "frontend", "environment" -> environment.entryName))
              )
            ),
            spec = Option(
              PodSpec(
                containers = Seq(
                  Container(
                    name = "nginx",
                    image = "nginx",
                    resources = Option(
                      ResourceRequirements(
                        Option(Map("cpu" -> Quantity("100m"), "memory" -> Quantity("128Mi"))),
                        Option(Map("cpu" -> Quantity("80m"), "memory" -> Quantity("64Mi")))
                      )
                    ),
                    volumeMounts = Option(Seq(VolumeMount(name = "nginx-config", mountPath = "/etc/nginx/conf.d"))),
                    ports = Option(Seq(ContainerPort(name = Option("http"), containerPort = 8080)))
                  ),
                  Container(
                    name = "web-backend",
                    image = s"registry.drivetribe.com/app/web-backend:$version",
                    resources = Option(
                      ResourceRequirements(
                        Option(Map("cpu" -> Quantity("125m"), "memory" -> Quantity("256Mi"))),
                        Option(Map("cpu" -> Quantity("100m"), "memory" -> Quantity("256Mi")))
                      )
                    ),
                    envFrom = Option(Seq(EnvFromSource(configMapRef = Option(ConfigMapEnvSource(Option("env-vars"))))))
                  )
                ),
                volumes = Option(
                  Seq(
                    Volume(
                      name = "nginx-config",
                      configMap = Option(ConfigMapVolumeSource(name = Option("nginx-config")))
                    )
                  )
                )
              )
            )
          )
        )
      )
    )

    Await.result(kube.namespaces(environment.entryName).deployments.create(deployment), Duration.Inf)
  }

  def deployOnBeanstalk(version: String, environment: Environment) = {
    val tfState = TerraformState.fromS3(environment)
    val applicationName =
      tfState.getResourceAttribute(Seq("root", "web"), "aws_elastic_beanstalk_environment.backend", "application")
    val beanstalkEnvironment =
      tfState.getResourceAttribute(Seq("root", "web"), "aws_elastic_beanstalk_environment.backend", "name")

    if (!ElasticBeanstalk.isAlreadyDeployed(applicationName, version, environment)) {
      ElasticBeanstalk.createApplicationVersion(applicationName, version, environment)
    }

    // Waiting beanstalk to be ready
    ElasticBeanstalk.waitEnvironmentToBeReady(applicationName, beanstalkEnvironment)

    // Ask to beanstalk to update environment version
    ElasticBeanstalk.updateEnvironmentVersion(applicationName, beanstalkEnvironment, version, environment)

    // Waiting beanstalk to finish the update
    ElasticBeanstalk.waitEnvironmentToBeReady(applicationName, beanstalkEnvironment)

    // Check if deployment succeeded
    if (ElasticBeanstalk.isDeploymentSuccess(applicationName, beanstalkEnvironment, version, environment))
      throw new IllegalStateException("Web deployment failed on AWS ElasticBeanstalk")
  }
}
