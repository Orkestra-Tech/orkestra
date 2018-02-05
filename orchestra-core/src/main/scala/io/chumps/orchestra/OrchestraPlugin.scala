package io.chumps.orchestra

import scala.concurrent.Future

import io.chumps.orchestra.model.RunInfo

trait OrchestraPlugin {
  def onMasterStart(): Future[Unit] = Future.unit
  def onJobStart(runInfo: RunInfo): Future[Unit] = Future.unit
}
