package com.drivetribe.orchestration

import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.Lowercase

sealed trait EnvironmentType extends EnumEntry with Lowercase
object EnvironmentType extends Enum[EnvironmentType] {
  case object Small extends EnvironmentType
  case object Medium extends EnvironmentType
  case object Large extends EnvironmentType
  val values = findValues
}
