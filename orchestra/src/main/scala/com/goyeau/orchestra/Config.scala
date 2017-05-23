package com.goyeau.orchestra

object Config {
  def apply(envVar: String) = Option(System.getenv(s"ORCHESTRA_$envVar")).filter(_.nonEmpty)
}
