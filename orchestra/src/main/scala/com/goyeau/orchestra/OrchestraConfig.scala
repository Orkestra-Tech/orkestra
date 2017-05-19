package com.goyeau.orchestra

object OrchestraConfig {
  val home = Config("ORCHESTRA_HOME").getOrElse(System.getProperty("user.home"))
}
