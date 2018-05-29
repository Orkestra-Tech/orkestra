package com.goyeau.orchestra.filesystem

object Implicits {
  implicit lazy val workDir: Directory = Directory(".")
}
