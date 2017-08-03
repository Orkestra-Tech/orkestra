package com.goyeau.orchestra

import java.io._
import java.net.URLEncoder

import scala.sys.process

import io.fabric8.kubernetes.client.dsl.ExecListener
import com.goyeau.orchestra.filesystem.Directory
import com.goyeau.orchestra.kubernetes.{Container, Kubernetes}
import okhttp3.Response
import scala.collection.convert.ImplicitConversionsToScala._

trait ShellHelpers {

  def sh(script: String)(implicit workDir: Directory): String =
    printMkString(process.Process(Seq("sh", "-c", script), workDir.file).lineStream)

  def sh(script: String, container: Container)(implicit workDir: Directory): String = {
    val outStream = new PipedOutputStream()
    val listener = new ExecListener {
      override def onFailure(t: Throwable, response: Response): Unit = finished()
      override def onClose(code: Int, reason: String): Unit = finished()
      override def onOpen(response: Response): Unit = ()
      private def finished() = {
        outStream.flush()
        outStream.close()
      }
    }

    Kubernetes.client.pods
      .inNamespace(Config.namespace)
      .withName(Config.podName)
      .inContainer(container.name)
      .redirectingInput()
      .writingOutput(outStream)
      .writingError(outStream)
      .withTTY()
      .usingListener(listener)
      .exec("sh", "-c", URLEncoder.encode(s"cd ${workDir.file.getAbsolutePath} && $script", "UTF-8"))

    val reader = new BufferedReader(new InputStreamReader(new PipedInputStream(outStream)))
    try printMkString(reader.lines.iterator.toStream, Option(container))
    finally reader.close()
  }

  private def printMkString(stream: Iterable[String], container: Option[Container] = None) = {
    val exitCodeRegex = ".*command terminated with non-zero exit code: Error executing in Docker Container: (\\d+).*".r

    stream.fold("") { (acc, line) =>
      println(line)
      line match {
        case exitCodeRegex(exitCode) =>
          throw new RuntimeException(
            s"Nonzero exit value: $exitCode${container.fold("")(c => s" in container ${c.name}")}"
          )
        case _ => s"$acc\n$line"
      }
    }
  }
}
