package com.goyeau.orchestra

trait Parameter[T] {
  def name: String
}
case class Param[T: Encoder](name: String) extends Parameter[T]

object RunId extends Parameter[String] {
  val name = "runId"
}
