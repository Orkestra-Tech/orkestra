package com.drivetribe.orchestration.backend

import com.drivetribe.orchestration._
import com.drivetribe.orchestration.infrastructure.Colour
import com.drivetribe.orchestration.infrastructure._
import com.goyeau.orchestra.{Job, _}
import com.typesafe.scalalogging.Logger
import shapeless.HNil

object SwitchActiveColour {

  def jobDefinition(environment: Environment) = Job[() => Unit](Symbol(s"switchActiveColour$environment"))

  def job(environment: Environment) = jobDefinition(environment).apply(apply(environment) _)

  def board(environment: Environment) = JobBoard("Switch Active Colour", jobDefinition(environment))

  private lazy val logger = Logger(getClass)

  def apply(environment: Environment)(): Unit = {
    val activeColour = Colour.getActive(environment)
    val inactiveColour = activeColour.opposite
    logger.info(s"Switching from * $activeColour * to * $inactiveColour *")

    val tfState = TerraformState.fromS3(environment)
    val activeLoadBalancer = tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.active", "arn")
    val inactiveLoadBalancer = tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.inactive", "arn")

    val monitoringActiveLoadBalancer =
      tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.monitoring_active", "arn")
    val monitoringInactiveLoadBalancer =
      tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.monitoring_inactive", "arn")

    val activeAutoScaling =
      tfState.getResourceAttribute(Seq("root", s"rest_api_${activeColour.entryName}"),
                                   "aws_autoscaling_group.api",
                                   "name")
    val inactiveAutoScaling =
      tfState.getResourceAttribute(Seq("root", s"rest_api_${inactiveColour.entryName}"),
                                   "aws_autoscaling_group.api",
                                   "name")

    stage("Scale up inactive") {
      AutoScaling.setDesiredCapacity(inactiveAutoScaling, AutoScaling.getDesiredCapacity(activeAutoScaling))
    }

    stage("Attach inactive") {
      AutoScaling.detachTargetGroups(inactiveAutoScaling, Seq(inactiveLoadBalancer, monitoringInactiveLoadBalancer))
      AutoScaling.attachTargetGroups(inactiveAutoScaling, Seq(activeLoadBalancer, monitoringActiveLoadBalancer))
    }

    stage("Wait") {
      while (!Colour.isHealthy(activeLoadBalancer)) Thread.sleep(5000)
      while (!AutoScaling.isDesiredCapacity(inactiveAutoScaling)) Thread.sleep(5000)
    }

    stage("Detach active") {
      AutoScaling.detachTargetGroups(activeAutoScaling, Seq(activeLoadBalancer, monitoringActiveLoadBalancer))
      AutoScaling.attachTargetGroups(activeAutoScaling, Seq(inactiveLoadBalancer, monitoringInactiveLoadBalancer))
    }

    logger.info(s"Switched from * $activeColour * to * $inactiveColour *")
  }
}
