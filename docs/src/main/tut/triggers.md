---
layout: docs
title:  "Triggering jobs"
position: 8
---

# Triggering jobs

Sometimes to achieve a goal we need multiple jobs to run. This is why we have the job triggers.

We can trigger a job by calling `.trigger()` on the `Job` we want to trigger. Note that this a fire and forget
action:
```tut:silent
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
// We import the triggers DSL
import com.goyeau.orchestra.utils.Triggers._

lazy val helloJobBoard = JobBoard[() => Unit](JobId("helloWorld"), "Hello World")()
lazy val helloJob = Job(helloJobBoard) { implicit workDir => () =>
  println("Hello World")
}

lazy val triggerJobBoard = JobBoard[() => Unit](JobId("trigger"), "Trigger")()
lazy val triggerJob = Job(triggerJobBoard) { implicit workDir => () =>
  // Let's trigger the Hello World job!
  helloJob.trigger()
}
```

If you'd like to run a job and therefore await the result of it call `.run()`. This will return the result in a
`Future`:
```tut:silent
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
// We import the triggers DSL
import com.goyeau.orchestra.utils.Triggers._

lazy val runJobBoard = JobBoard[() => Unit](JobId("run"), "Run")()
lazy val runJob = Job(runJobBoard) { implicit workDir => () =>
  // Let's run the Hello World job, which will trigger and return the result in a Future
  val result: Future[Unit] = helloJob.run()

  // We can also await that the triggered job has completed by awaiting on the Future
  Await.result(result, 1.minute)
}

lazy val helloJobBoard = JobBoard[() => Unit](JobId("helloWorld"), "Hello World")()
lazy val helloJob = Job(helloJobBoard) { implicit workDir => () =>
  println("Hello World")
}
```

`.trigger()` or `.run()` will trigger or run the job with the same RunId as the triggering job. That means they will
share the same log history. You will also see the stages of the other job in the run history but they will be dimmed.
