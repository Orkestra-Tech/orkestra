---
layout: docs
title:  "Triggering jobs"
position: 3
---

# Triggering jobs

Sometimes to achieve a goal we need multiple jobs to run. This is why the id of a run (RunId) is not a sub-resource of a
job. That means multiple jobs can run under the same id and therefore they will share the same log history.

A good metaphor is the file system tree vs the labelling system that GMail uses. A file or directory can only be under
one folder whereas an email or another label can be under multiple label.

We can trigger a job by calling `.trigger()` on the job we want to trigger. Note that this a fire and forget action:
```tut:silent
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

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

If you'd like to run a job and therefore await the result of it call `.run()`. This will return the result in a Future:
```tut:silent
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

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
