package com.goyeau.orchestra.io

import java.io.File

trait DirectoryHelpers {

  def dir[T](path: String)(func: Directory => T)(implicit parentDir: Directory): T =
    dir(new File(path))(func)

  def dir[T](path: File)(func: Directory => T)(implicit parentDir: Directory): T =
    func(
      if (path.isAbsolute) Directory(path)
      else parentDir / path
    )
}
