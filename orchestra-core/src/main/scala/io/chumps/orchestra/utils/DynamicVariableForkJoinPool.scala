package io.chumps.orchestra.utils

import java.util.concurrent.ForkJoinPool

class DynamicVariableForkJoinPool() extends ForkJoinPool {
  override def execute(task: Runnable): Unit = {
    val outValue = Console.out
    val errValue = Console.err
    val stageValue = ElasticsearchOutputStream.stageVar.value

    super.execute { () =>
      Console.withOut(outValue) {
        Console.withErr(errValue) {
          ElasticsearchOutputStream.stageVar.withValue(stageValue) {
            task.run()
          }
        }
      }
    }
  }
}
