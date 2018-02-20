package io.chumps.orchestra.filesystem

import java.io.File

trait DirectoryUtils {

  def dir[Result](path: String)(func: Directory => Result)(implicit parentDir: Directory): Result =
    dir(new File(path))(func)

  def dir[Result](path: File)(func: Directory => Result)(implicit parentDir: Directory): Result =
    func(
      if (path.isAbsolute) Directory(path)
      else parentDir / path
    )
}

object DirectoryUtils extends DirectoryUtils
