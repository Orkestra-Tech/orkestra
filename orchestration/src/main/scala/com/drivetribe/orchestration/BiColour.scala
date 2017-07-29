package com.drivetribe.orchestration

import com.amazonaws.regions.Regions
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest
import scala.collection.convert.ImplicitConversions._

import com.amazonaws.services.ec2.model.{DescribeTagsRequest, Filter}

object BiColour {

  def getActiveColour(environment: Environment): EnvironmentColour = {
    val tfState = TerraformState.fromS3(environment)
    val activeLoadBalancer = tfState.getResourceAttribute(Seq("root"), "aws_alb_target_group.active", "arn")

    val elb = AmazonElasticLoadBalancingClientBuilder.standard().withRegion(Regions.EU_WEST_1).build
    val instanceIds = elb
      .describeTargetHealth(new DescribeTargetHealthRequest().withTargetGroupArn(activeLoadBalancer))
      .getTargetHealthDescriptions
      .map(_.getTarget.getId)

    val ec2 = AmazonEC2ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build
    val instances = ec2.describeTags(
      new DescribeTagsRequest(Seq(new Filter("key", Seq("Colour")), new Filter("resource-id", instanceIds)))
    )
    val instanceColours = instances.getTags.map(_.getValue)
    assert(instanceColours.distinct.size == 1)

    EnvironmentColour.withNameInsensitive(instanceColours.head)
  }
}
