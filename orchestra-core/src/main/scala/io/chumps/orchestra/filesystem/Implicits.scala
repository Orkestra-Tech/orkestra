package io.chumps.orchestra.filesystem

object Implicits {
  implicit lazy val workDir: Directory = Directory(".")
}
