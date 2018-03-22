package com.drivetribe.orchestra.filesystem

object Implicits {
  implicit lazy val workDir: Directory = Directory(".")
}
