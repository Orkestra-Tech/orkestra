package com.goyeau.orchestra

import java.util.UUID

import enumeratum._

sealed trait Parameter[T] {
  lazy val id: Symbol = Symbol(name.toLowerCase.replaceAll("\\s", ""))
  def name: String
  def defaultValue: Option[T]
}

case class Param[T](name: String, defaultValue: Option[T] = None) extends Parameter[T]

case class EnumParam[Entry <: EnumEntry](name: String, enum: Enum[Entry], defaultValue: Option[Entry] = None)
    extends Parameter[Entry]

object RunId extends Parameter[UUID] {
  val name = "Run ID"
  def defaultValue = None
}
