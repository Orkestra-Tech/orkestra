---
layout: docs
title:  "Cron jobs"
---

# Cron jobs

Orkestra supports Cron jobs via the `orkestra-cron` library/plugin.
```scala
libraryDependencies += "com.goyeau" %%% "orkestra-cron" % orkestraVersion
```

Orkestra Cron jobs have a one to one mapping to [Kubernetes CronJobs](https://kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/).

To add cron support, we mix in the trait `CronTriggers`, which requires us to implement
`cronTriggers: Set[CronTrigger]`.

Let's write a job that is triggered every 5min:
```tut:silent
import com.goyeau.orkestra._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
// We import the Cron package
import com.goyeau.orkestra.cron._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._

object Orkestra extends OrkestraServer with CronTriggers { // Note that we mix in GithubHooks
  lazy val board = Folder("Orkestra")(cronJobBoard)
  lazy val jobs = Set(cronJob) // We still need to add the Job to jobs

  // We add the CronTrigger to the cronTriggers
  lazy val cronTriggers = Set(CronTrigger("*/5 * * * *", cronJob)())

  lazy val cronJobBoard = JobBoard[() => Unit](JobId("cron"), "Cron")()
  lazy val cronJob = Job(cronJobBoard) { implicit workDir => () =>
    println("Hey!")
  }
}
```
