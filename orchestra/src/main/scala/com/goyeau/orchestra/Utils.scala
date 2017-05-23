package com.goyeau.orchestra

import java.io.PrintStream

object Utils {

  def withOutErr[T](out: PrintStream)(func: => T): T = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(out)
      System.setErr(out)
      Console.withOut(out)(Console.withErr(out)(func))
    } finally {
      out.flush()
      out.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }
}
