---
layout: docs
title:  "RunId"
position: 10
---

# RunId

It is possible to get the run id of the job, for example if you want to send a Slack message with a link to the job:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

lazy val runIdJob = Job[RunId => Unit](JobId("runId"), "RunId")()
lazy val runIdJobRunner = JobRunner(runIdJob) { implicit workDir => runId =>
  println(s"My run id is ${runId.value}")
}
```

The RunId is an automatically generated UUID, unless we specify it as a UI parameter like this:  
`http://<url to your job>?runId=<some UUID>`  
So in the following example we will display in the logs a link to trigger another job that will have the same RunId.
We usually do this when we'd like the approval of a user to continue the process and keep the run of the 2 jobs like one
continuous run:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

lazy val firstJob = Job[RunId => Unit](JobId("first"), "First")()
lazy val firstJobRunner = JobRunner(firstJob) { implicit workDir => runId =>
  println(s"Trigger the next job: http://orchestra.company.com/orchestra/second?runId=${runId.value}")
}

lazy val secondJob = Job[() => Unit](JobId("second"), "Second")()
lazy val secondJobRunner = JobRunner(secondJob) { implicit workDir => () =>
  println("Second job running")
}
```
