package com.goyeau.orchestra

import io.circe.generic.auto._

object OrchestraConfig {
  val home = Config("HOME").getOrElse(System.getProperty("user.home"))
  val port = Config("PORT").map(_.toInt)
  val runInfo = Config("RUN_INFO").map(AutowireServer.read[RunInfo])
}
