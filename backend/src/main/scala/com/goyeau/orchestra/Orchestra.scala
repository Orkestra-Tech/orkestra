package com.goyeau.orchestra

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{HttpApp, Route}
import play.twirl.api.Html
import com.goyeau.orchestra._

case class Orchestra(name: String) extends HttpApp {
  implicit val twirlHtmlMarshaller: ToEntityMarshaller[Html] =
    Marshaller.StringMarshaller.wrap(`text/html`)(_.toString)
  implicit val executionContext = systemReference.get().dispatcher

  def route: Route = //Root(name)(displayItems: _*).route
    pathSingleSlash {
      complete {
        html.index.render()
      }
    } ~
      pathPrefix("assets" / Remaining) { file =>
        // optionally compresses the response with Gzip or Deflate
        // if the client accepts compressed responses
        encodeResponse {
          getFromResource(s"public/$file")
        }
      } ~
      path("api" / Segments) { segments =>
        post(AutowireServer.dispatch(segments))
      }
}
