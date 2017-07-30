package com.drivetribe.orchestration.frontend

import java.io.File
import java.nio.file.{Files, Paths}

import scala.collection.convert.ImplicitConversions._

import com.amazonaws.regions.Regions
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.drivetribe.orchestration.infrastructure.Environment

object ElasticBeanstalk {
  private lazy val elasticBeanstalk = AWSElasticBeanstalkClientBuilder.standard.withRegion(Regions.EU_WEST_1).build
  private def versionLabel(version: String, environment: Environment) = s"${version}_${environment.entryName}"

  def isAlreadyDeployed(applicationName: String, version: String, environment: Environment) =
    elasticBeanstalk
      .describeApplicationVersions(
        new DescribeApplicationVersionsRequest()
          .withApplicationName(applicationName)
          .withVersionLabels(versionLabel(version, environment))
      )
      .getApplicationVersions
      .nonEmpty

  def createApplicationVersion(applicationName: String, version: String, environment: Environment) = {
    val transferManager = TransferManagerBuilder.defaultTransferManager
    val s3Bucket = "drivetribe-web-releases"
    val s3Key = s"web-backend-${version}_${environment.entryName}.json"
    Files.write(Paths.get(s3Key), beanstalkBundle(version, environment).lines.toSeq)
    transferManager.upload(s3Bucket, s3Key, new File(s3Key))

    elasticBeanstalk.createApplicationVersion(
      new CreateApplicationVersionRequest(applicationName, versionLabel(version, environment))
        .withSourceBundle(new S3Location(s3Bucket, s3Key))
    )
  }

  def waitEnvironmentToBeReady(applicationName: String, beanstalkEnvironment: String) =
    while (getEnvironment(applicationName, beanstalkEnvironment).forall(_.getStatus == "Ready")) Thread.sleep(20000)

  def getEnvironment(applicationName: String, beanstalkEnvironment: String) =
    elasticBeanstalk
      .describeEnvironments(
        new DescribeEnvironmentsRequest()
          .withApplicationName(applicationName)
          .withEnvironmentNames(beanstalkEnvironment)
      )
      .getEnvironments
      .headOption

  def isDeploymentSuccess(applicationName: String,
                          beanstalkEnvironment: String,
                          version: String,
                          environment: Environment) =
    getEnvironment(applicationName, beanstalkEnvironment).forall(
      _.getVersionLabel == versionLabel(version, environment)
    )

  def updateEnvironmentVersion(applicationName: String,
                               beanstalkEnvironment: String,
                               version: String,
                               environment: Environment) =
    elasticBeanstalk
      .updateEnvironment(
        new UpdateEnvironmentRequest()
          .withApplicationName(applicationName)
          .withEnvironmentName(beanstalkEnvironment)
          .withVersionLabel(versionLabel(version, environment))
      )
      .getStatus

  def beanstalkBundle(version: String, environment: Environment) =
    s"""{
       |  "AWSEBDockerrunVersion": 2,
       |  "containerDefinitions": [
       |    {
       |      "name": "nginx",
       |      "image": "registry.drivetribe.com/app/web-nginx:$version",
       |      "essential": true,
       |      "memory": 64,
       |      "portMappings": [
       |        {
       |          "hostPort": 80,
       |          "containerPort": 8080
       |        }
       |      ],
       |      "links": [
       |        "node"
       |      ]
       |    },
       |    {
       |      "name": "node",
       |      "image": "registry.drivetribe.com/app/web-backend:$version",
       |      "essential": true,
       |      "memory": ${if (environment.isProd) 768 else 400}
       |    },
       |    {
       |      "name": "dd-agent",
       |      "image": "datadog/docker-dd-agent:latest",
       |      "memory": 256,
       |      "environment": [
       |        {
       |          "name": "API_KEY",
       |          "value": "${System.getenv("DATADOG_TOKEN")}"
       |        },
       |        {
       |          "name": "SD_BACKEND",
       |          "value": "docker"
       |        },
       |        {
       |          "name": "TAGS",
       |          "value": "environment:${environment.entryName},service:web"
       |        }
       |      ],
       |      "mountPoints": [
       |        {
       |          "sourceVolume": "docker",
       |          "containerPath": "/var/run/docker.sock",
       |          "readOnly": true
       |        },
       |        {
       |          "sourceVolume": "proc",
       |          "containerPath": "/host/proc",
       |          "readOnly": true
       |        },
       |        {
       |          "sourceVolume": "cgroup",
       |          "containerPath": "/host/sys/fs/cgroup",
       |          "readOnly": true
       |        }
       |      ]
       |    }
       |  ],
       |  "volumes": [
       |    {
       |      "name": "docker",
       |      "host": {
       |        "sourcePath": "/var/run/docker.sock"
       |      }
       |    },
       |    {
       |      "name": "proc",
       |      "host": {
       |        "sourcePath": "/proc"
       |      }
       |    },
       |    {
       |      "name": "cgroup",
       |      "host": {
       |        "sourcePath": "/sys/fs/cgroup"
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin
}
