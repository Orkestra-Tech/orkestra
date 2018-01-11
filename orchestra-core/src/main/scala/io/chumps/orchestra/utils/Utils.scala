package io.chumps.orchestra.utils

import java.io.PrintStream
import java.nio.file.{Files, Paths}
import java.time.{LocalDateTime, ZoneOffset}

import scala.concurrent.Await
import scala.concurrent.duration._

import io.chumps.orchestra.{Elasticsearch, OrchestraConfig}
import io.chumps.orchestra.model.{RunId, RunInfo}
import io.chumps.orchestra.model.Indexed.LogsIndex

object Utils {

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[Result](stream: PrintStream)(f: => Result): Result = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(stream)
      System.setErr(stream)
      Console.withOut(stream)(Console.withErr(stream)(f))
    } finally {
      stream.flush()
      stream.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }

  def elasticsearchOutErr[Result](runId: RunId)(f: => Result): Result =
    withOutErr(
      new PrintStream(
        new ElasticsearchOutputStream(Await.result(Elasticsearch.client, 1.minute), LogsIndex.index, runId)
      )
    )(f)

  def runInit(runInfo: RunInfo, tags: Seq[String]): Unit = {
    val runDir = OrchestraConfig.jobRunDir(runInfo)
    val firstTimeInit = runDir.toFile.mkdirs()

    if (firstTimeInit) {
      OrchestraConfig.runDir(runInfo.runId).toFile.mkdirs()

      tags.foreach { tag =>
        val tagDir = OrchestraConfig.tagDir(runInfo.jobId, tag)
        tagDir.toFile.mkdirs()
        Files.createSymbolicLink(Paths.get(tagDir.toString, runInfo.runId.value.toString), runDir)
      }

      val now = LocalDateTime.now()
      val dateDir = Paths
        .get(OrchestraConfig.runsByDateDir(runInfo.jobId).toString,
             now.getYear.toString,
             now.getDayOfYear.toString,
             now.toEpochSecond(ZoneOffset.UTC).toString)
      dateDir.toFile.mkdirs()
      Files.createSymbolicLink(Paths.get(dateDir.toString, runInfo.runId.value.toString), runDir)
    }
  }

  def generateColour(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
