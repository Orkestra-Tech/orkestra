package io.chumps.orchestra

import java.io.PrintStream

object Utils {

  /** Sets the standard out and err across all thread.
    * This is not Thread safe!
    */
  def withOutErr[T](stream: PrintStream)(func: => T): T = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(stream)
      System.setErr(stream)
      Console.withOut(stream)(Console.withErr(stream)(func))
    } finally {
      stream.flush()
      stream.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }
}
