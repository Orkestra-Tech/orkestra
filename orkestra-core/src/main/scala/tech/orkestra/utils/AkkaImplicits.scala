package tech.orkestra.utils

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

trait AkkaImplicits {
  implicit lazy val system: ActorSystem = ActorSystem("orkestra")
  implicit lazy val materializer: Materializer = ActorMaterializer()
  implicit lazy val executionContext: ExecutionContext = system.dispatcher
}

object AkkaImplicits extends AkkaImplicits
