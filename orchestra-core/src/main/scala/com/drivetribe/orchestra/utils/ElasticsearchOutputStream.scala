package com.drivetribe.orchestra.utils

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

import com.drivetribe.orchestra.model.Indexed.LogLine
import com.drivetribe.orchestra.model.Indexed.LogsIndex
import com.drivetribe.orchestra.model.RunId
import com.drivetribe.orchestra.utils.AkkaImplicits._

class ElasticsearchOutputStream(client: HttpClient, runId: RunId) extends OutputStream {
  private val lineBuffer = new DynamicVariable(new StringBuffer())
  private implicit val requestBuilder: RequestBuilder[LogLine] = indexInto(LogsIndex.index, LogsIndex.`type`).source(_)
  private val batchSize = 50
  private val elasticsearchSink =
    Sink.fromSubscriber(
      client.subscriber[LogLine](batchSize = batchSize, concurrentRequests = 1, flushInterval = Option(1.second))
    )
  private val linesStream = Source.queue(batchSize * 2, OverflowStrategy.backpressure).to(elasticsearchSink).run()
  private val position = Iterator.from(0)

  override def write(byte: Int): Unit =
    lineBuffer.value.appendCodePoint(byte)

  override def write(bytes: Array[Byte], offset: Int, length: Int): Unit =
    lineBuffer.value.append(new String(bytes, offset, length))

  override def flush(): Unit = {
    val newLineChar = "\n"
    while (lineBuffer.value.toString.contains(newLineChar)) {
      val newLineIndex = lineBuffer.value.indexOf(newLineChar)
      val line = lineBuffer.value.substring(0, newLineIndex)
      val logline = LogLine(runId, Instant.now(), position.next(), Secrets.sanitize(line))
      Await.result(linesStream.offer(logline), 1.minute)
      lineBuffer.value.delete(0, newLineIndex + 1)
    }
  }

  override def close(): Unit = {
    flush()
    linesStream.complete()
    Await.result(linesStream.watchCompletion(), 1.minute)
  }
}
