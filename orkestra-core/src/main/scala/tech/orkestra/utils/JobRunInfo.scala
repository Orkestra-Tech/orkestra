package tech.orkestra.utils

import tech.orkestra.OrkestraConfig
import tech.orkestra.model.RunId

trait JobRunInfo {
  protected def orkestraConfig: OrkestraConfig

  def runId: RunId = orkestraConfig.runInfo.runId
}

object JobRunInfo extends JobRunInfo {
  override lazy val orkestraConfig: OrkestraConfig = OrkestraConfig.fromEnvVars()
}
