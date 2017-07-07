package com.goyeau.orchestration

import enumeratum._

sealed trait Environment extends EnumEntry
object Environment extends Enum[Environment] {
  case object Nardo extends Environment
  val values = findValues
}
