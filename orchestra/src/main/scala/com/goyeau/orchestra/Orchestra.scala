package com.goyeau.orchestra

import scala.scalajs.js.JSApp

import com.goyeau.orchestra.Job.Runner
import com.goyeau.orchestra.css.AppCSS
import com.goyeau.orchestra.routes.{Backend, WebRouter}
import io.circe.shapes.HListInstances
import org.scalajs.dom

trait Orchestra extends JSApp with HListInstances {

  def jobs: Seq[Runner[_, _, _]]
  def board: Board

  // Web main
  override def main(): Unit = {
    AppCSS.load
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById("orchestra"))
  }

  // Backend main
  def main(args: Array[String]): Unit =
    OrchestraConfig.runInfo.fold {
      val port = OrchestraConfig.port.getOrElse(throw new IllegalStateException("ORCHESTRA_PORT should be set"))
      Backend(jobs).startServer("0.0.0.0", port)
    } { runInfo =>
      jobs
        .find(_.definition.id == runInfo.job)
        .getOrElse(throw new IllegalArgumentException(s"No job found for id ${runInfo.job}"))
        .run(runInfo)
    }
}
