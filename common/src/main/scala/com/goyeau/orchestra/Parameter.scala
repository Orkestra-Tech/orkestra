package com.goyeau.orchestra

trait Parameter[T]
trait DisplayableParameter {
  def name: String
}

case class Param[T](name: String) extends Parameter[T] with DisplayableParameter

object RunId extends Parameter[String]

object Dummy extends Parameter[Unit]
