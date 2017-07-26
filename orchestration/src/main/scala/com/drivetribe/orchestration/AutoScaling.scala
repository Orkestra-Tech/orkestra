package com.drivetribe.orchestration

import com.amazonaws.regions.Regions
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import scala.collection.convert.ImplicitConversions._

object AutoScaling {

  def getDesiredCapacity(autoScalingName: String): Int = {
    val autoscaling = AmazonAutoScalingClientBuilder.standard().withRegion(Regions.EU_WEST_1).build
    val request: DescribeAutoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest()
    request.setAutoScalingGroupNames(Seq(autoScalingName))
    autoscaling
      .describeAutoScalingGroups(request)
      .getAutoScalingGroups
      .headOption
      .fold(throw new IllegalStateException(s"Unable to get the auto scaling group for $autoScalingName"))(
        _.getDesiredCapacity
      )
  }
}
