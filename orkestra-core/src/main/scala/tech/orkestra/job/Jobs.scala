package tech.orkestra.job

import java.io.{IOException, PrintStream}
import java.time.Instant

import cats.implicits._
import cats.effect.{Async, Sync}

import scala.util.DynamicVariable
import com.sksamuel.elastic4s.cats.effect.instances._
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.http.ElasticClient
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.update.UpdateResponse
import io.circe.{Encoder, Json}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.java8.time._
import tech.orkestra.model.Indexed._
import tech.orkestra.model.RunInfo
import tech.orkestra.utils.BaseEncoders._

private[orkestra] object Jobs {

  def pong[F[_]: Async](runInfo: RunInfo)(implicit elasticsearchClient: ElasticClient): F[UpdateResponse] =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("latestUpdateOn" -> Instant.now().asJson))
      )
      .to[F]
      .map(response => response.fold(throw new IOException(response.error.reason))(identity))

  def succeedJob[F[_]: Async, Result: Encoder](runInfo: RunInfo, result: Result)(
    implicit elasticsearchClient: ElasticClient
  ): F[UpdateResponse] =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("result" -> Option(Right(result): Either[Throwable, Result]).asJson))
          .retryOnConflict(1)
      )
      .to[F]
      .map(response => response.fold(throw new IOException(response.error.reason))(identity))

  def failJob[F[_]: Async, Result](runInfo: RunInfo, throwable: Throwable)(
    implicit elasticsearchClient: ElasticClient
  ): F[Result] =
    elasticsearchClient
      .execute(
        updateById(HistoryIndex.index.name, HistoryIndex.`type`, HistoryIndex.formatId(runInfo))
          .doc(Json.obj("result" -> Option(Left(throwable): Either[Throwable, Unit]).asJson))
          .retryOnConflict(1)
      )
      .to[F]
      .flatMap(_ => Sync[F].raiseError[Result](throwable))

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[Result](stream: PrintStream)(f: => Result): Result = {
    val systemOut = System.out
    val systemErr = System.err
    val outVarField = Console.getClass.getDeclaredField("outVar")
    outVarField.setAccessible(true)
    val consoleOut = Console.out
    val errVarField = Console.getClass.getDeclaredField("errVar")
    errVarField.setAccessible(true)
    val consoleErr = Console.err

    try {
      System.setOut(stream)
      System.setErr(stream)
      outVarField.set(Console, new DynamicVariable(stream))
      errVarField.set(Console, new DynamicVariable(stream))
      Console.withOut(stream)(Console.withErr(stream)(f))
    } finally {
      stream.flush()
      stream.close()
      System.setOut(systemOut)
      System.setErr(systemErr)
      outVarField.set(Console, new DynamicVariable(consoleOut))
      errVarField.set(Console, new DynamicVariable(consoleErr))
    }
  }
}
