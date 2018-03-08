package io.chumps.orchestra.utils

import java.util.concurrent.ForkJoinPool

class DynamicVariableForkJoinPool() extends ForkJoinPool {
  override def execute(task: Runnable): Unit = {
    val out = Console.out
    val err = Console.err
    val stage = ElasticsearchOutputStream.stageVar.value

    super.execute { () =>
      Console.withOut(out) {
        Console.withErr(err) {
          ElasticsearchOutputStream.stageVar.withValue(stage) {
            task.run()
          }
        }
      }
    }
  }
}
