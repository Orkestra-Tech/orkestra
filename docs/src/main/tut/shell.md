---
layout: docs
title:  "Shell"
position: 4
---

# Shell

There is 2 ways of running a shell script, async or blocking. The async way is recommended in functional programming but
if you are coming from a more DevOps background this might blow you mind.

## Async shell
```tut:silent
import scala.concurrent.Await
import scala.concurrent.duration._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
// We import the shells DSL
import com.drivetribe.orchestra.utils.Shells._

lazy val shellJob = Job[() => Unit](JobId("shell"), "Shell")()
lazy val shellJobRunner = JobRunner(shellJob) { implicit workDir => () =>
  Await.result(sh("echo 'Hi mate!'"), 1.second)
}
```

## Blocking shell
```tut:silent
import scala.concurrent.Await
import scala.concurrent.duration._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
// We import the blocking shell DSL
import com.drivetribe.orchestra.utils.BlockingShells._

lazy val shellJob = Job[() => Unit](JobId("shell"), "Shell")()
lazy val shellJobRunner = JobRunner(shellJob) { implicit workDir => () =>
  sh("echo 'Hi mate!'")
}
```