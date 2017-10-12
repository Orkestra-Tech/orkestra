package io.chumps.orchestra

import java.util.UUID

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom.ext.Ajax
import shapeless.HList

import io.chumps.orchestra.kubernetes.Kubernetes

trait CommonApi {
  def logs(runId: UUID, page: Page[Int]): Seq[(Option[Symbol], String)]
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
  import AkkaImplicits._

  override def logs(runId: UUID, page: Page[Int]): Seq[(Option[Symbol], String)] = {
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

  override def runningJobs(): Seq[RunInfo] = {
    val jobList = Await.result(Kubernetes.client.jobs.namespace(OrchestraConfig.namespace).list(), Duration.Inf)
    for {
      job <- jobList.items
      jobSpec <- job.spec
      podSpec <- jobSpec.template.spec
      container <- podSpec.containers.headOption
      envs <- container.env
      env <- envs.find(_.name == "ORCHESTRA_RUN_INFO")
      runInfoJson <- env.value
    } yield RunInfo.decodeWithFallbackRunId(runInfoJson, UUID.fromString(job.metadata.get.uid.get))
  }
}
