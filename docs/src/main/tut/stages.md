---
layout: docs
title:  "Stages"
position: 2
---

# Stages

Stages are more UI related elements to show some sort of progress of the job. It also shows the time spent in the stage:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

lazy val stagesJob = Job[() => Unit](JobId("stages"), "Stages")()
lazy val stagesJobRunner = JobRunner(stagesJob) { implicit workDir => () =>
  stage("Stage 1") {
    println("Doing stage 1")
  }

  stage("Stage 2") {
    println("Doing stage 2")
  }
}
```

Here is a more meaningful example of the UI with the stages:  
<img alt="Stages" srcset="img/stages.png 2x">
