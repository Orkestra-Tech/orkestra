package io.chumps.orchestra.utils

import java.time.Instant

import scala.util.DynamicVariable

import io.chumps.orchestra.AStageStatus._
import io.chumps.orchestra.{AStageStatus, OrchestraConfig}

trait StagesHelpers {

  def stage[Result](name: String)(f: => Result) = {
    val runInfo = OrchestraConfig.runInfo
    AStageStatus.persist(runInfo.runId, StageStart(name, Instant.now()))
    try StagesHelpers.stageVar.withValue(Option(Symbol(name))) {
      println(s"Stage: $name")
      f
    } finally AStageStatus.persist(runInfo.runId, StageEnd(name, Instant.now()))
  }
}

object StagesHelpers {
  private[orchestra] val stageVar = new DynamicVariable[Option[Symbol]](None)
}
