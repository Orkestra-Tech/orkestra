package com.goyeau.orchestra.kubernetes

import java.io.File

case class Directory(file: File) {
  def /(subPath: String): Directory = Directory(new File(file, subPath))

  def /(subPath: File): Directory = this / subPath.getPath
}

object Directory {
  implicit val default = Directory(".")

  def apply(path: String): Directory = Directory(new File(path))
}

trait DirectoryHelpers {

  def dir[T](path: String)(func: Directory => T)(implicit workDir: Directory): T =
    dir(new File(path))(func)

  def dir[T](path: File)(func: Directory => T)(implicit parentDir: Directory): T =
    func(
      if (path.isAbsolute) Directory(path)
      else parentDir / path
    )
}
