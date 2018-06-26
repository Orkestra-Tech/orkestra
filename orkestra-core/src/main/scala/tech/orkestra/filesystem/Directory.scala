package tech.orkestra.filesystem

import java.io.File
import java.nio.file.{Path, Paths}

case class Directory(path: Path) {
  def /(subPath: Path): Directory = Directory(path.resolve(subPath))
  def /(subPath: File): Directory = this / subPath.toPath
}

object Directory {
  def apply(path: String): Directory = Directory(Paths.get(path))
}
