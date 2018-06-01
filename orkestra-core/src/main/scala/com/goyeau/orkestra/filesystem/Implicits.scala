package com.goyeau.orkestra.filesystem

object Implicits {
  implicit lazy val workDir: Directory = Directory(".")
}
