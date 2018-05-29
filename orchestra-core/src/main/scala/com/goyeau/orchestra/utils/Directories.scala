package com.goyeau.orchestra.utils

import java.io.File

import com.goyeau.orchestra.filesystem.Directory

trait Directories {

  def dir[Result](path: String)(func: Directory => Result)(implicit parentDir: Directory): Result =
    dir(new File(path))(func)

  def dir[Result](path: File)(func: Directory => Result)(implicit parentDir: Directory): Result =
    func(parentDir / path)
}

object Directories extends Directories
