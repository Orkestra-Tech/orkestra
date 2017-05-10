package com.goyeau.orchestra

import java.io.PrintStream

object Utils {

  def withOutErr(out: PrintStream)(func: => Unit): Unit = {
    val stdOut = System.out
    val stdErr = System.err
    try {
      System.setOut(out)
      System.setErr(out)
      scala.Console.withOut(out)(scala.Console.withErr(out)(func))
    } finally {
      out.flush()
      out.close()
      System.setOut(stdOut)
      System.setErr(stdErr)
    }
  }
}
