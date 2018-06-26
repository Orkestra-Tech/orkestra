package tech.orkestra.filesystem

object Implicits {
  implicit lazy val workDir: Directory = Directory(".")
}
