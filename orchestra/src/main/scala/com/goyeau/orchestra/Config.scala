package com.goyeau.orchestra

object Config {
  def apply(envVar: String) = Option(System.getenv(envVar)).filter(_.nonEmpty)
}
