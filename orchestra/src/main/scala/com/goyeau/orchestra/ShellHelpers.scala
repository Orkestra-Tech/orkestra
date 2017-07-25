package com.goyeau.orchestra

import java.io._
import java.net.URLEncoder

import scala.sys.process

import _root_.io.fabric8.kubernetes.client.dsl.ExecListener
import com.goyeau.orchestra.io.Directory
import com.goyeau.orchestra.kubernetes.Kubernetes
import okhttp3.Response
import scala.collection.convert.ImplicitConversions._

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
    try printMkString(reader.lines.iterator.toStream)
    finally reader.close()
  }

  def printMkString(stream: Iterable[String]) = stream.fold("") { (acc, line) =>
    println(line)
    s"$acc\n$line"
  }
}
