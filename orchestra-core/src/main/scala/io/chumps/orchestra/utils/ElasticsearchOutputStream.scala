package io.chumps.orchestra.utils

import java.io.OutputStream
import java.time.Instant

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.DynamicVariable

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.streams.ReactiveElastic._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.streams.RequestBuilder
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.model.Indexed.LogLine
import io.chumps.orchestra.model.Indexed.LogsIndex
import io.chumps.orchestra.model.RunId
import io.chumps.orchestra.utils.AkkaImplicits._

class ElasticsearchOutputStream(client: HttpClient, runId: RunId) extends OutputStream {
  private val lineBuffer = new DynamicVariable(new StringBuffer())
  private implicit val requestBuilder: RequestBuilder[LogLine] = indexInto(LogsIndex.index, LogsIndex.`type`).source(_)
  private val bufferSize = 100
  private val elasticsearchSink =
    Sink.fromSubscriber(
      client.subscriber[LogLine](batchSize = bufferSize, concurrentRequests = 1, flushInterval = Option(1.second))
    )
  private val linesStream = Source.queue(bufferSize, OverflowStrategy.backpressure).to(elasticsearchSink).run()

  override def write(byte: Int): Unit =
    lineBuffer.value.appendCodePoint(byte)

  override def write(bytes: Array[Byte], offset: Int, length: Int): Unit =
    lineBuffer.value.append(new String(bytes, offset, length))

  override def flush(): Unit = {
    val newLineChar = "\n"
    while (lineBuffer.value.toString.contains(newLineChar)) {
      val newLineIndex = lineBuffer.value.indexOf(newLineChar)
      val line = lineBuffer.value.substring(0, newLineIndex)
      val logline = LogLine(runId, Instant.now(), line, ElasticsearchOutputStream.stageVar.value)
      Await.result(linesStream.offer(logline), 1.minute)
      lineBuffer.value.delete(0, newLineIndex + 1)
    }
  }

  override def close(): Unit = flush()
}

object ElasticsearchOutputStream {
  private[orchestra] val stageVar = new DynamicVariable[Option[String]](None)
}
