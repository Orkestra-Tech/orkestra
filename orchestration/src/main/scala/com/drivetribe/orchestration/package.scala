package com.drivetribe

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

package object orchestration {

  implicit class SeqFutureOps[+T](futures: Seq[Future[T]]) {
    def parallel(implicit ec: ExecutionContext) = Await.result(Future.sequence(futures), Duration.Inf)
  }
}
