---
layout: docs
title:  "Shell scripts"
position: 5
---

# Shell scripts

There are 2 ways of running a shell script, async or blocking. The async way is recommended in functional programming
but if you are coming from a more DevOps background this might blow you mind.

## Blocking shell

The `BlockingShells` provides us a function `sh` that will run the shell script you give and returns the output in a
`String`:
```tut:silent
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._
// We import the blocking shell DSL
import com.goyeau.orkestra.utils.BlockingShells._

lazy val shellJobBoard = JobBoard[() => Unit](JobId("shell"), "Shell")()
lazy val shellJob = Job(shellJobBoard) { implicit workDir => () =>
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
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._
// We import the shell DSL
import com.goyeau.orkestra.utils.Shells._

lazy val shellJobBoard = JobBoard[() => Unit](JobId("shell"), "Shell")()
lazy val shellJob = Job(shellJobBoard) { implicit workDir => () =>
  val mate = sh("echo 'Hi mate!'")
  val sir = sh("echo 'Hello sir'")
  
  // Await the result of the 2 Futures in parallel
  Await.result(Future.sequence(Seq(mate, sir)), 1.second)
}
```
