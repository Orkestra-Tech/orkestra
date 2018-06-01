package com.goyeau.orkestra

import com.goyeau.orkestra.board.Board
import com.goyeau.orkestra.css.AppCss
import com.goyeau.orkestra.route.WebRouter
import com.goyeau.kubernetesclient.KubernetesClient
import com.sksamuel.elastic4s.http.HttpClient
import org.scalajs.dom

/**
  * Mix in this trait to create the Orkestra server.
  */
trait OrkestraServer extends OrkestraPlugin {
  implicit override def orkestraConfig: OrkestraConfig = ???
  implicit override def kubernetesClient: KubernetesClient = ???
  implicit override def elasticsearchClient: HttpClient = ???

  def board: Board

  def main(args: Array[String]): Unit = {
    AppCss.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(BuildInfo.projectName.toLowerCase))
  }
}
