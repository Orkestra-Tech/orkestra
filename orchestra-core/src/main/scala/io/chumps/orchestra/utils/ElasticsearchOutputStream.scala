package io.chumps.orchestra.utils

import java.io.OutputStream
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._

import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.model.Indexed.LogLine
import io.chumps.orchestra.model.Indexed.LogsIndex
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.utils.AkkaImplicits._

class ElasticsearchOutputStream(client: HttpClient, runId: RunId) extends OutputStream {
  private val buffer = new StringBuffer()
  private val scheduledFlush = system.scheduler.schedule(1.second, 1.second) {
    val index = buffer.lastIndexOf("\n") + 1
    if (index != 0) {
      flush(buffer.substring(0, index))
      buffer.delete(0, index)
    }
  }

  override def write(b: Int): Unit = buffer.appendCodePoint(b)

  override def write(b: Array[Byte], off: Int, len: Int): Unit =
    buffer.append(b.slice(off, off + len).map(_.toChar))

  override def flush(): Unit = {
    flush(buffer.toString)
    buffer.delete(0, buffer.length)
  }

  def flush(buffer: String): Unit =
    Await.result(
      client.execute(
        bulk(
          buffer.toString.split("\\r?\\n").map { line =>
            indexInto(LogsIndex.index, LogsIndex.`type`)
              .source(LogLine(runId, Instant.now(), line, StagesHelpers.stageVar.value))
          }
        )
      ),
      1.minute
    )

  override def close(): Unit = {
    scheduledFlush.cancel()
    flush()
    client.close()
  }
}
