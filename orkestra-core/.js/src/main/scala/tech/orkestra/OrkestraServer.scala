package tech.orkestra

import tech.orkestra.board.Board
import tech.orkestra.css.AppCss
import tech.orkestra.route.WebRouter
import com.goyeau.kubernetes.client.KubernetesClient
import com.sksamuel.elastic4s.http.ElasticClient
import org.scalajs.dom

/**
  * Mix in this trait to create the Orkestra server.
  */
trait OrkestraServer[F[_]] extends OrkestraPlugin {
  implicit override def orkestraConfig: OrkestraConfig = ???
  implicit override def kubernetesClient: KubernetesClient = ???
  implicit override def elasticsearchClient: ElasticClient = ???

  def board: Board

  def main(args: Array[String]): Unit = {
    AppCss.load()
    WebRouter.router(board).renderIntoDOM(dom.document.getElementById(BuildInfo.projectName.toLowerCase))
  }
}
