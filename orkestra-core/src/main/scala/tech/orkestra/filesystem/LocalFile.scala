package tech.orkestra.filesystem

import java.io.File
import java.nio.file.{Path, Paths}

case class LocalFile(workDir: Directory, path: Path) extends File(workDir.path.resolve(path).toUri)

object LocalFile {
  def apply(path: Path)(implicit workDir: Directory): LocalFile = LocalFile(workDir, path)
  def apply(file: File)(implicit workDir: Directory): LocalFile = apply(file.toPath)
  def apply(path: String)(implicit workDir: Directory): LocalFile = apply(Paths.get(path))
}
