package com.goyeau.orchestration

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed abstract class Environment(val environmentType: EnvironmentType) extends EnumEntry
object Environment extends Enum[Environment] {
  case object Nardo extends Environment(EnvironmentType.Medium)
  val values = findValues
}

sealed trait EnvironmentType extends EnumEntry with Lowercase
object EnvironmentType extends Enum[EnvironmentType] {
  case object Small extends EnvironmentType
  case object Medium extends EnvironmentType
  case object Large extends EnvironmentType
  val values = findValues
}
