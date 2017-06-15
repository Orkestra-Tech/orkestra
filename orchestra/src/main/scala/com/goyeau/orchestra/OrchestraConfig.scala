package com.goyeau.orchestra

import java.io.File

import io.circe.generic.auto._

object OrchestraConfig {
  val workspace = new File("workspace") // TODO maybe we should move that somewhere else

  val home = Config("HOME").getOrElse(System.getProperty("user.home"))
  val port = Config("PORT").map(_.toInt)
  val runInfo = Config("RUN_INFO").map(AutowireServer.read[RunInfo])
}
