package io.chumps.orchestra.filesystem

import java.io.File

case class Directory(file: File) {
  def /(subPath: String): Directory = Directory(new File(file, subPath))

  def /(subPath: File): Directory = this / subPath.getPath
}

object Directory {
  def apply(path: String): Directory = Directory(new File(path))
}
