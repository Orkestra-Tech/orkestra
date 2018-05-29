---
layout: docs
title:  "Stages"
position: 4
---

# Stages

Stages are more UI related elements to show some sort of progress of the job. It also shows the time spent in the stage:
```tut:silent
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
// We import the stages DSL
import com.goyeau.orchestra.utils.Stages._

lazy val stagesJobBoard = JobBoard[() => Unit](JobId("stages"), "Stages")()
lazy val stagesJob = Job(stagesJobBoard) { implicit workDir => () =>
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
The stages in this screenshot are Checks, Publish and Deploy. The colour black, blue and green on them is generated.
