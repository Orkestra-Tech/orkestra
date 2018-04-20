---
layout: docs
title:  "Triggering jobs"
position: 8
---

# Triggering jobs

Sometimes to achieve a goal we need multiple jobs to run. This is why we have the job triggers.

We can trigger a job by calling `.trigger()` on the `JobRunner` we want to trigger. Note that this a fire and forget
action:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
// We import the triggers DSL
import com.drivetribe.orchestra.utils.Triggers._

lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
  println("Hello World")
}

lazy val triggerJob = Job[() => Unit](JobId("trigger"), "Trigger")()
lazy val triggerJobRunner = JobRunner(triggerJob) { implicit workDir => () =>
  // Let's trigger the Hello World job!
  helloJobRunner.trigger()
}
```

If you'd like to run a job and therefore await the result of it call `.run()`. This will return the result in a
`Future`:
```tut:silent
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
// We import the triggers DSL
import com.drivetribe.orchestra.utils.Triggers._

lazy val runJob = Job[() => Unit](JobId("run"), "Run")()
lazy val runJobRunner = JobRunner(runJob) { implicit workDir => () =>
  // Let's run the Hello World job, which will trigger and return the result in a Future
  val result: Future[Unit] = helloJobRunner.run()

  // We can also await that the triggered job has completed by awaiting on the Future
  Await.result(result, 1.minute)
}

lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
  println("Hello World")
}
```

`.trigger()` or `.run()` will trigger or run the job with the same RunId as the triggering job. That means they will
share the same log history. You will also see the stages of the other job in the run history but they will be dimmed.
