package com.drivetribe.orchestration.infrastructure

import scala.collection.convert.ImplicitConversions._

import com.amazonaws.regions.Regions
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder
import com.amazonaws.services.autoscaling.model.{
  AttachLoadBalancerTargetGroupsRequest,
  DescribeAutoScalingGroupsRequest,
  DetachLoadBalancerTargetGroupsRequest,
  SetDesiredCapacityRequest
}

object AutoScaling {

  lazy val autoscaling = AmazonAutoScalingClientBuilder.standard().withRegion(Regions.EU_WEST_1).build

  def getDesiredCapacity(autoScalingName: String): Int =
    autoscaling
      .describeAutoScalingGroups(
        new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(Seq(autoScalingName))
      )
      .getAutoScalingGroups
      .headOption
      .fold(throw new IllegalStateException(s"Unable to get the auto scaling group for $autoScalingName"))(
        _.getDesiredCapacity
      )

  def setDesiredCapacity(autoScalingName: String, capacity: Int) =
    autoscaling.setDesiredCapacity(
      new SetDesiredCapacityRequest().withAutoScalingGroupName(autoScalingName).withDesiredCapacity(capacity)
    )

  def attachTargetGroups(autoScalingName: String, targetGroupArns: Seq[String]) =
    autoscaling.attachLoadBalancerTargetGroups(
      new AttachLoadBalancerTargetGroupsRequest()
        .withAutoScalingGroupName(autoScalingName)
        .withTargetGroupARNs(targetGroupArns)
    )

  def detachTargetGroups(autoScalingName: String, targetGroupArns: Seq[String]) =
    autoscaling.detachLoadBalancerTargetGroups(
      new DetachLoadBalancerTargetGroupsRequest()
        .withAutoScalingGroupName(autoScalingName)
        .withTargetGroupARNs(targetGroupArns)
    )

  def isDesiredCapacity(autoScalingName: String) =
    autoscaling
      .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingName))
      .getAutoScalingGroups
      .head
      .getInstances
      .map(_.getLifecycleState)
      .forall(_ == "InService")
}
