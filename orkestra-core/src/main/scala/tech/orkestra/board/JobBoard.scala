package tech.orkestra.board

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}
import tech.orkestra.OrkestraConfig
import tech.orkestra.model._
import tech.orkestra.input.InputOperations
import tech.orkestra.utils.{AutowireClient, AutowireServer}
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.java8.time._
import shapeless._

trait JobBoard[Parameters <: HList] extends Board {
  val id: JobId
  val segment = id.value
  val name: String

  private[orkestra] trait Api {
    def trigger(
      runId: RunId,
      params: Parameters,
      tags: Seq[String] = Seq.empty,
      by: Option[RunInfo] = None
    ): Future[Unit]
    def stop(runId: RunId): Future[Unit]
    def tags(): Future[Seq[String]]
    def history(page: Page[Instant]): Future[History[Parameters]]
  }

  private[orkestra] object Api {
    def router(apiServer: Api)(implicit ec: ExecutionContext, decoder: Decoder[Parameters]) =
      AutowireServer.route[Api](apiServer)

    val client = AutowireClient(s"${OrkestraConfig.jobSegment}/${id.value}")[Api]
  }
}

object JobBoard {

  /**
    * Create a JobBoard.
    * A JobBoard defines the type of the function it runs, a unique id and a pretty name for the UI.
    *
    * @param id A unique JobId
    * @param name A pretty name for the display
    */
  def apply[Params <: HList, Parameters <: HList](id: JobId, name: String)(params: Params)(
    implicit paramOperations: InputOperations[Params, Parameters],
    encoder: Encoder[Parameters],
    decoder: Decoder[Parameters]
  ) =
    SimpleJobBoard[Params, Parameters](id, name, params)
}
