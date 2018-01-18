package io.chumps.orchestra.utils

import java.io.OutputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.DynamicVariable

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
  private val stringBuffer = new DynamicVariable(new StringBuffer())
  private val lineBuffer = new AtomicReference(Seq.empty[LogLine])
  private val scheduledFlush = system.scheduler.schedule(1.second, 1.second)(flush())

  private def bufferLine() = {
    val index = stringBuffer.value.lastIndexOf("\n") + 1
    if (index != 0) {
      lineBuffer.updateAndGet(
        _ ++ stringBuffer.value.substring(0, index).split("\\n").map { line =>
          LogLine(runId, Instant.now(), line, StagesHelpers.stageVar.value)
        }
      )
      stringBuffer.value.delete(0, index)
    }
  }

  override def write(byte: Int): Unit = {
    stringBuffer.value.appendCodePoint(byte)
    bufferLine()
  }

  override def write(bytes: Array[Byte], offset: Int, length: Int): Unit = {
    stringBuffer.value.append(new String(bytes, offset, length))
    bufferLine()
  }

  override def flush(): Unit =
    Await.result(
      client.execute(
        bulk(
          lineBuffer.getAndSet(Seq.empty).map(logLine => indexInto(LogsIndex.index, LogsIndex.`type`).source(logLine))
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
