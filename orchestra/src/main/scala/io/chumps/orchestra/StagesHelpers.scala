package io.chumps.orchestra

import java.io.{OutputStream, PrintStream}
import java.time.Instant

import scala.util.DynamicVariable

import io.chumps.orchestra.AStageStatus._

trait StagesHelpers {

  def stage[T](name: String)(f: => T) = {
    val runInfo =
      OrchestraConfig.runInfo.getOrElse(throw new IllegalStateException("ORCHESTRA_RUN_INFO should be set"))

    AStageStatus.persist(runInfo.runId, StageStart(name, Instant.now()))
    try StagesHelpers.stageVar.withValue(Option(Symbol(name)))(f)
    finally AStageStatus.persist(runInfo.runId, StageEnd(name, Instant.now()))
  }
}

object StagesHelpers {

  private[orchestra] val delimiter = "_ColumnDelimiter_"
  private[orchestra] val stageVar = new DynamicVariable[Option[Symbol]](None)

  def apply(out: OutputStream) = new LogsPrintStream(out)

  class LogsPrintStream(out: OutputStream) extends PrintStream(out, true) {
    private def stageInfo() = stageVar.value.map(stageId => s"$delimiter${stageId.name}")
    private def insertStageInfo(s: String) =
      stageInfo().fold(s)(added => s.replaceAll("\r", "").replaceAll("\n", s"$added\n"))

    override def print(s: String): Unit = super.print(insertStageInfo(s))
    override def println(s: String): Unit = super.println(s + stageInfo().mkString)
    override def println(o: Any): Unit = println(String.valueOf(o))
    override def println(): Unit = super.println(stageInfo().mkString)
  }
}
