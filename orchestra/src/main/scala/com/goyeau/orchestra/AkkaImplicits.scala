package com.goyeau.orchestra

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object AkkaImplicits {
  implicit lazy val system = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val dispatcher = system.dispatcher
}
