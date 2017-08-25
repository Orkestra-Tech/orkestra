package com.drivetribe.orchestration.infrastructure

import java.io.{BufferedReader, InputStreamReader}

import scala.collection.convert.ImplicitConversionsToScala._

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.drivetribe.orchestration.Environment
import io.circe.Json
import io.circe.parser._

case class TerraformState(raw: Json) {

  def getResourceAttribute(modulePath: Seq[String], resource: String, attribute: String): String =
    raw.hcursor
      .downField("modules")
      .downAt(_.hcursor.downField("path").values.toVector.flatten.flatMap(_.asString) == modulePath)
      .downField("resources")
      .downField(resource)
      .downField("primary")
      .downField("attributes")
      .downField(attribute)
      .as[String]
      .fold(throw _, identity)
}

object TerraformState {

  def fromS3(environment: Environment): TerraformState = {
    // @TODO Pull state
    val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.EU_WEST_1).build
    val obj = s3.getObject("drivetribe-terraform", s"tfstates/app-${environment.entryName}.tfstate")
    val objContent = new BufferedReader(new InputStreamReader(obj.getObjectContent)).lines().iterator.mkString
    val objJson = parse(objContent).fold(throw _, identity)
    TerraformState(objJson)
  }
}
