package com.goyeau.orchestra

import scala.util.Random

trait Parameter[T] {
  def name: String
  def defaultValue: Option[T]
  def getValue(valueMap: Map[String, Any]): T =
    valueMap
      .get(name)
      .map(_.asInstanceOf[T])
      .orElse(defaultValue)
      .getOrElse(throw new IllegalArgumentException(s"Can't get param $name"))
}

case class Param[T](name: String, defaultValue: Option[T] = None) extends Parameter[T]

object RunId extends Parameter[String] {
  val name = "runId"
  def defaultValue = Some(Random.nextInt().toString) // Generate a Slug Id instead
}
