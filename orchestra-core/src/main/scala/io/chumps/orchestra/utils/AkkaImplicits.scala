package com.drivetribe.orchestra.utils

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

object AkkaImplicits {
  implicit lazy val system: ActorSystem = ActorSystem("orchestra")
  implicit lazy val materializer: Materializer = ActorMaterializer()
  implicit lazy val executionContext: ExecutionContext = system.dispatcher
}
