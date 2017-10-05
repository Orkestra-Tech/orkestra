package io.chumps.orchestra.filesystem

import java.io.File

case class LocalFile(workDir: Directory, path: String) extends File(workDir.file, path)

object LocalFile {
  def apply(path: String)(implicit workDir: Directory): LocalFile = LocalFile(workDir, path)
}
