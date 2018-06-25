package com.goyeau.orkestra.utils

import com.goyeau.orkestra.OrkestraConfig
import com.goyeau.orkestra.model.RunId

trait JobRunInfo {
  protected def orkestraConfig: OrkestraConfig

  def runId: RunId = orkestraConfig.runInfo.runId
}

object JobRunInfo extends JobRunInfo {
  override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
}
