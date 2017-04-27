package com.goyeau.orchestra

import scala.concurrent.Future

trait Api {
  def runTask(taskId: Symbol): Future[Unit]
}
