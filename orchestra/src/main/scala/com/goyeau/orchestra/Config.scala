package com.goyeau.orchestra

object Config {
  val home = Option(System.getenv("ORCHESTRA_HOME"))
    .filter(_.nonEmpty)
    .getOrElse(System.getProperty("user.home"))

}
