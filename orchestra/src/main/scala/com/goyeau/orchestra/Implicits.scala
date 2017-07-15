package com.goyeau.orchestra

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait Implicits {
  implicit lazy val system = ActorSystem()
  implicit lazy val materializer = ActorMaterializer()
  implicit lazy val dispatcher = system.dispatcher
}

object Implicits extends Implicits
