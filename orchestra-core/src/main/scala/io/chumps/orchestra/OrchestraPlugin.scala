package io.chumps.orchestra

import io.chumps.orchestra.model.RunInfo

trait OrchestraPlugin {
  def onMasterStart(): Unit = {}
  def onJobStart(runInfo: RunInfo): Unit = {}
}
