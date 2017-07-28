package com.goyeau.orchestra

import java.util.UUID

sealed trait Parameter[T] {
  lazy val id: String = name.toLowerCase.replaceAll("\\s", "")
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

object RunId extends Parameter[UUID] {
  val name = "runId"
  def defaultValue = None
}
