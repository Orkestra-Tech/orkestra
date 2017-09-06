package com.drivetribe.orchestration

import java.net.URI

import enumeratum.EnumEntry.Lowercase
import enumeratum._

sealed abstract class Environment(val environmentType: EnvironmentType,
                                  val isProd: Boolean,
                                  val frontend: URI,
                                  val monitoringApi: URI)
    extends EnumEntry
    with Lowercase {
  val nonProd = !isProd
  val isBiColour = environmentType == EnvironmentType.Large
}

object Environment extends Enum[Environment] {
  case object Staging
      extends Environment(
        EnvironmentType.Large,
        true,
        URI.create("https://staging.drivetribe.com"),
        URI.create("https://api-monitoring.drivetribe.com")
      )
  case object Aragon
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://aragon.drivetribe.com"),
        URI.create("https://aragon-api.drivetribe.com:6443")
      )
  case object Baku
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://baku.drivetribe.com"),
        URI.create("https://baku-api.drivetribe.com:6443")
      )
  case object Balocco
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://balocco.drivetribe.com"),
        URI.create("https://balocco-api.drivetribe.com:6443")
      )
  case object Cartagena
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://cartagena.drivetribe.com"),
        URI.create("https://cartagena-api.drivetribe.com:6443")
      )
  case object Estoril
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://estoril.drivetribe.com"),
        URI.create("https://estoril-api.drivetribe.com:6443")
      )
  case object LeMans
      extends Environment(
        EnvironmentType.Large,
        false,
        URI.create("https://lemans.drivetribe.com"),
        URI.create("https://lemans-api-monitoring.drivetribe.com")
      )
  case object Monaco
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://monaco.drivetribe.com"),
        URI.create("https://monaco-api.drivetribe.com:6443")
      )
  case object Monza
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://monza.drivetribe.com"),
        URI.create("https://monza-api.drivetribe.com:6443")
      )
  case object Nardo
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://nardo.drivetribe.com"),
        URI.create("https://nardo-api.drivetribe.com:6443")
      )
  case object Ovale
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://ovale.drivetribe.com"),
        URI.create("https://ovale-api.drivetribe.com:6443")
      )
  case object Sakhir
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://sakir.drivetribe.com"),
        URI.create("https://sakir-api.drivetribe.com:6443")
      )
  case object SanMarino
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://sanmarino.drivetribe.com"),
        URI.create("https://sanmarino-api.drivetribe.com:6443")
      )
  case object Spa
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://spa.drivetribe.com"),
        URI.create("https://spa-api.drivetribe.com:6443")
      )
  case object Suzuka
      extends Environment(
        EnvironmentType.Medium,
        false,
        URI.create("https://suzuka.drivetribe.com"),
        URI.create("https://suzuka-api.drivetribe.com:6443")
      )
  val values = findValues
}
