package io.chumps.orchestra.utils

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

object AkkaImplicits {
  implicit lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(
    new DynamicVariableForkJoinPool()
  )
  implicit lazy val system: ActorSystem = ActorSystem("orchestra", defaultExecutionContext = Option(executionContext))
  implicit lazy val materializer: Materializer = ActorMaterializer()
}
