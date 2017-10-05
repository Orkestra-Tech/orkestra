package io.chumps.orchestra

import java.io.{OutputStream, PrintStream}

import scala.util.DynamicVariable

trait LoggingHelpers {

  def stage[T](name: String)(f: => T) =
    LoggingHelpers.stageVar.withValue(Option(Symbol(name)))(f)
}

object LoggingHelpers {

  private[orchestra] val delimiter = "_OrchestraDelimiter_"
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
