package tech.orkestra

import cats.effect.{ExitCode, IO, IOApp}
import tech.orkestra.board.Board
import tech.orkestra.css.AppCss
import tech.orkestra.route.WebRouter
import com.sksamuel.elastic4s.http.ElasticClient
import org.scalajs.dom

/**
  * Mix in this trait to create the Orkestra server.
  */
trait OrkestraServer extends IOApp with OrkestraPlugin[IO] {
  implicit override def orkestraConfig: OrkestraConfig = ???
  implicit override def elasticsearchClient: ElasticClient = ???

  def board: Board

  def run(args: List[String]): IO[ExitCode] = IO {
    AppCss.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(BuildInfo.projectName.toLowerCase))
    ExitCode.Success
  }
}
