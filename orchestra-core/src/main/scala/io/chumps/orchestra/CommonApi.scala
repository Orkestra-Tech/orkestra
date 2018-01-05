package io.chumps.orchestra

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax

import io.chumps.orchestra.kubernetes.Kubernetes
import io.chumps.orchestra.model.{Page, RunId, RunInfo}
import io.chumps.orchestra.utils.StagesHelpers

trait CommonApi {
  def logs(runId: RunId, page: Page[Int]): Seq[(Option[Symbol], String)]
  def runningJobs(): Seq[RunInfo]
}

object CommonApi {
  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  val client = new autowire.Client[String, Decoder, Encoder] {

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

}

object CommonApiServer extends CommonApi {
  import io.chumps.orchestra.utils.AkkaImplicits._

  override def logs(runId: RunId, page: Page[Int]): Seq[(Option[Symbol], String)] = {
    val stageRegex = s"(.*)${StagesHelpers.delimiter}(.+)".r
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

  override def runningJobs(): Seq[RunInfo] =
    Await.result(
      Kubernetes.client.jobs.namespace(OrchestraConfig.namespace).list().map(_.items.map(RunInfo.fromKubeJob)),
      1.minute
    )
}
