package com.drivetribe.orchestration.infrastructure

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed abstract class Environment(val environmentType: EnvironmentType, val isProd: Boolean)
    extends EnumEntry
    with Lowercase {
  val nonProd = !isProd
  val isBiColour = environmentType == EnvironmentType.Large
}

object Environment extends Enum[Environment] {
  case object Staging extends Environment(EnvironmentType.Large, true)
  case object Aragon extends Environment(EnvironmentType.Medium, false)
  case object Baku extends Environment(EnvironmentType.Medium, false)
  case object Balocco extends Environment(EnvironmentType.Medium, false)
  case object Cartagena extends Environment(EnvironmentType.Medium, false)
  case object Estoril extends Environment(EnvironmentType.Medium, false)
  case object LeMans extends Environment(EnvironmentType.Large, false)
  case object Monaco extends Environment(EnvironmentType.Medium, false)
  case object Monza extends Environment(EnvironmentType.Medium, false)
  case object Nardo extends Environment(EnvironmentType.Medium, false)
  case object Ovale extends Environment(EnvironmentType.Medium, false)
  case object Sakir extends Environment(EnvironmentType.Medium, false)
  case object SanMarino extends Environment(EnvironmentType.Medium, false)
  case object Spa extends Environment(EnvironmentType.Medium, false)
  case object Suzuka extends Environment(EnvironmentType.Medium, false)
  val values = findValues
}

sealed trait EnvironmentType extends EnumEntry with Lowercase
object EnvironmentType extends Enum[EnvironmentType] {
  case object Small extends EnvironmentType
  case object Medium extends EnvironmentType
  case object Large extends EnvironmentType
  val values = findValues
}

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
