package io.chumps.orchestra.utils

import java.io.PrintStream

import io.chumps.orchestra.model.RunId

object Utils {

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[Result](stream: PrintStream)(f: => Result): Result = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(stream)
      System.setErr(stream)
      Console.withOut(stream)(Console.withErr(stream)(f))
    } finally {
      stream.flush()
      stream.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }

  def elasticsearchOutErr[Result](runId: RunId)(f: => Result): Result =
    withOutErr(new PrintStream(new ElasticsearchOutputStream(Elasticsearch.client, runId)))(f)

  def generateColour(s: String): String = {
    def hex(shift: Int) =
      Integer.toHexString((s.hashCode >> shift) & 0x5) // 0x5 instead of 0xF to keep the colour dark
    "#" + hex(20) + hex(16) + hex(12) + hex(8) + hex(4) + hex(0)
  }
}
