---
layout: docs
title:  "Shell scripts"
position: 3
---

# Shell scripts

There are 2 ways of running a shell script, async or blocking. The async way is recommended in functional programming
but if you are coming from a more DevOps background this might blow you mind.

## Blocking shell
The `BlockingShells` provides us a function `sh` that will run the shell script you give and returns the output in a
`String`:
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

## Async shell
The `Shells` object is the same as `BlockingShells` but instead the function `sh` returns a `Future[String]`, where
the `Future` completes when the shell execution is done. This is especially interesting when you want to run shell
scripts in parallel like in this example:
```tut:silent
import scala.concurrent.{Await, Future}
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
  val mate = sh("echo 'Hi mate!'")
  val sir = sh("echo 'Hello sir'")
  
  // Await the result of the 2 Futures in parallel
  Await.result(Future.sequence(Seq(mate, sir)), 1.second)
}
```
