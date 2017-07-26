package com.drivetribe.orchestration

import com.amazonaws.services.s3.AmazonS3ClientBuilder

object BiColour {

  def getActiveColour(environment: Environment): EnvironmentColour = {
    val s3 = AmazonS3ClientBuilder.defaultClient()
    val o = s3.getObject("drivetribe-terraform", s"/tfstates/app-${environment.entryName}.tfstate")
    println(o)
    ???
  }
}
