package io.chumps.orchestra.utils

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

object AkkaImplicits {
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: Materializer = ActorMaterializer()
  implicit lazy val dispatcher: ExecutionContext = system.dispatcher
}
