package com.drivetribe.orchestration

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

sealed trait EnvironmentColour extends EnumEntry with Lowercase {
  def opposite: EnvironmentColour
}
object EnvironmentColour extends Enum[EnvironmentColour] {
  case object Black extends EnvironmentColour {
    override lazy val opposite: EnvironmentColour = White
  }
  case object White extends EnvironmentColour {
    override lazy val opposite: EnvironmentColour = Black
  }
  case object Common extends EnvironmentColour {
    override lazy val opposite: EnvironmentColour = this
  }
  val values = findValues
}

sealed trait EnvironmentSide extends EnumEntry with Lowercase {
  def opposite: EnvironmentSide
}
object EnvironmentSide extends Enum[EnvironmentSide] {
  case object Active extends EnvironmentSide {
    override lazy val opposite: EnvironmentSide = Inactive
  }
  case object Inactive extends EnvironmentSide {
    override lazy val opposite: EnvironmentSide = Active
  }
  case object Common extends EnvironmentSide {
    override lazy val opposite: EnvironmentSide = this
  }
  val values = findValues
}
