package io.chumps.orchestra.job

import java.io.{IOException, PrintStream}
import java.time.Instant

import scala.concurrent.Future
import scala.util.DynamicVariable

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.HttpClient
import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._

import io.chumps.orchestra.model.Indexed._
import io.chumps.orchestra.model.RunInfo
import io.chumps.orchestra.utils.AkkaImplicits._
import io.chumps.orchestra.utils.BaseEncoders._

object JobRunners {

  def pong(runInfo: RunInfo)(implicit elasticsearchClient: HttpClient) =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("latestUpdateOn" -> Instant.now().asJson))
      )
      .map(_.fold(failure => throw new IOException(failure.error.reason), identity))

  def succeedJob[Result: Encoder](runInfo: RunInfo, result: Result)(implicit elasticsearchClient: HttpClient) =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("result" -> Option(Right(result): Either[Throwable, Result]).asJson))
          .retryOnConflict(1)
      )
      .map(_.fold(failure => throw new IOException(failure.error.reason), identity))

  def failJob(runInfo: RunInfo, throwable: Throwable)(implicit elasticsearchClient: HttpClient) =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("result" -> Option(Left(throwable): Either[Throwable, Unit]).asJson))
          .retryOnConflict(1)
      )
      .flatMap(_ => Future.failed(throwable))

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[Result](stream: PrintStream)(f: => Result): Result = {
    val stdOut = System.out
    val stdErr = System.err
    val field = Console.getClass.getDeclaredField("outVar")
    field.setAccessible(true)
    val consoleStdOut = Console.out
    try {
      System.setOut(stream)
      System.setErr(stream)
      field.set(Console, new DynamicVariable(stream))
      Console.withOut(stream)(Console.withErr(stream)(f))
    } finally {
      field.set(Console, new DynamicVariable(consoleStdOut))
      stream.flush()
      stream.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }
}
