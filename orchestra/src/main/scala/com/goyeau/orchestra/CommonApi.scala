package com.goyeau.orchestra

import java.util.UUID

import scala.concurrent.Future
import scala.io.Source

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax

trait CommonApi {
  def logs(runId: UUID, page: Page[Int]): Seq[(Option[Symbol], String)]
  def runningJobs(): Seq[RunInfo]
}

object CommonApi extends CommonApi {

  val client = new autowire.Client[String, Decoder, Encoder] {
    import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

    override def doCall(req: Request): Future[String] =
      Ajax
        .post(
          url = (Jobs.apiSegment +: Jobs.commonSegment +: req.path).mkString("/"),
          data = req.args.asJson.noSpaces,
          responseType = "application/json",
          headers = Map("Content-Type" -> "application/json")
        )
        .map(_.responseText)

    override def read[T: Decoder](raw: String) = decode[T](raw).fold(throw _, identity)
    override def write[T: Encoder](obj: T) = obj.asJson.noSpaces
  }.apply[CommonApi]

  override def logs(runId: UUID, page: Page[Int]): Seq[(Option[Symbol], String)] = {
    val stageRegex = s"(.*)${LoggingHelpers.delimiter}(.+)".r
    Seq(OrchestraConfig.logsFile(runId).toFile)
      .filter(_.exists())
      .flatMap(
        Source
          .fromFile(_)
          .getLines()
          .slice(page.from.getOrElse(0), Int.MaxValue)
          .filter(_.nonEmpty)
          .map {
            case stageRegex(line, stage) => (Option(Symbol(stage)), line)
            case line                    => (None, line)
          }
          .toSeq
      )
  }

  override def runningJobs(): Seq[RunInfo] = Seq(
    RunInfo('test, Option(UUID.randomUUID())),
    RunInfo('test2, Option(UUID.randomUUID()))
  )
}
