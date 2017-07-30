package com.drivetribe.orchestration.frontend

import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit._
import java.util.Date

import scala.collection.convert.ImplicitConversionsToJava._

import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.{ObjectMetadataProvider, TransferManagerBuilder}
import com.drivetribe.orchestration.infrastructure._
import com.drivetribe.orchestration.{Lock, Project}
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import io.fabric8.kubernetes.api.model._
import io.fabric8.kubernetes.api.model.extensions.{
  Deployment,
  DeploymentSpec,
  DeploymentStrategy,
  RollingUpdateDeployment
}
import io.fabric8.kubernetes.client.DefaultKubernetesClient

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
    println("Deploy web frontend")

    val transferManager = TransferManagerBuilder.defaultTransferManager
    transferManager.download("drivetribe-web-releases", s"web-frontend-$version.zip", new File("."))

    sh(s"""mkdir temp
          |unzip web-frontend-$version.zip -d temp
          |""".stripMargin)

    val metadataProvider = new ObjectMetadataProvider {
      override def provideObjectMetadata(file: File, metadata: ObjectMetadata): Unit = {
        metadata.setExpirationTime(Date.from(Instant.now().plus(1, YEARS)))
        metadata.setCacheControl("max-age=31536000")
      }
    }

    transferManager.uploadDirectory(
      "drivetribe-live-frontend-application",
      version,
      new File("temp"),
      true,
      metadataProvider
    )
  }

  def webBackend(version: String, environment: Environment) = {
    println("Deploy web backend")
    if (environment.environmentType == EnvironmentType.Medium) deployOnKubernetes(version, environment)
    else deployOnBeanstalk(version, environment)
  }

  def deployOnKubernetes(version: String, environment: Environment) = {
    val kube = new DefaultKubernetesClient()

    val deployment = new Deployment()
    deployment.setMetadata {
      val meta = new ObjectMeta()
      meta.setName("web-backend")
      meta.setNamespace("environment")
      meta
    }
    deployment.setSpec {
      val spec = new DeploymentSpec()
      spec.setReplicas(2)
      spec.setStrategy {
        val strategy = new DeploymentStrategy()
        strategy.setType("RollingUpdate")
        strategy.setRollingUpdate(new RollingUpdateDeployment(new IntOrString("10%"), new IntOrString("50%")))
        strategy
      }
      spec.setTemplate(
        new PodTemplateSpec(
          {
            val meta = new ObjectMeta()
            meta.setLabels(Map("app" -> "web", "tier" -> "frontend", "environment" -> environment.entryName))
            meta
          }, {
            val spec = new PodSpec()
            spec.setContainers(
              Seq(
                {
                  val container = new Container()
                  container.setName("nginx")
                  container.setImage("nginx")
                  container.setResources(
                    new ResourceRequirements(
                      Map("cpu" -> new Quantity("100m"), "memory" -> new Quantity("128Mi")),
                      Map("cpu" -> new Quantity("80m"), "memory" -> new Quantity("64Mi"))
                    )
                  )
                  container.setVolumeMounts(Seq {
                    val volumeMount = new VolumeMount()
                    volumeMount.setName("nginx-config")
                    volumeMount.setMountPath("/etc/nginx/conf.d")
                    volumeMount
                  })
                  container.setPorts(Seq {
                    val port = new ContainerPort()
                    port.setName("http")
                    port.setContainerPort(8080)
                    port
                  })
                  container
                }, {
                  val container = new Container()
                  container.setName("web-backend")
                  container.setImage(s"registry.drivetribe.com/app/web-backend:$version")
                  container.setResources(
                    new ResourceRequirements(
                      Map("cpu" -> new Quantity("125m"), "memory" -> new Quantity("256Mi")),
                      Map("cpu" -> new Quantity("100m"), "memory" -> new Quantity("256Mi"))
                    )
                  )
                  container.setEnv(Seq {
                    val source = new EnvVarSource()
                    source.setConfigMapKeyRef(new ConfigMapKeySelector("name", "env-vars"))
                    val envVar = new EnvVar()
                    envVar.setValueFrom(source)
                    envVar
                  })
                  container
                }
              )
            )
            spec.setVolumes(
              Seq {
                val volume = new Volume()
                volume.setName("nginx-config")
                volume.setConfigMap {
                  val configMap = new ConfigMapVolumeSource()
                  configMap.setName("nginx-config")
                  configMap
                }
                volume
              }
            )
            spec
          }
        )
      )
      spec
    }

    kube.extensions.deployments.createOrReplace(deployment)
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
