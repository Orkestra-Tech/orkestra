---
layout: docs
title:  "RunId"
position: 10
---

# RunId

It is possible to get the run id of the job, for example if you want to send a Slack message with a link to the job:
```tut:silent
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.job._
import tech.orkestra.model._
// We import the job run info utils
import tech.orkestra.utils.JobRunInfo._

lazy val runIdJobBoard = JobBoard[() => Unit](JobId("runId"), "RunId")()
lazy val runIdJob = Job(runIdJobBoard) { implicit workDir => () =>
  println(s"My run id is ${runId.value}")
}
```

The RunId is an automatically generated UUID, unless we specify it as a UI parameter like this:  
`http://<url to your job>?runId=<some UUID>`  
So in the following example we will display in the logs a link to trigger another job that will have the same RunId.
We usually do this when we'd like the approval of a user to continue the process and keep the run of the 2 jobs like one
continuous run:
```tut:silent
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.job._
import tech.orkestra.model._
// We import the job run info utils
import tech.orkestra.utils.JobRunInfo._

lazy val firstJobBoard = JobBoard[() => Unit](JobId("first"), "First")()
lazy val firstJob = Job(firstJobBoard) { implicit workDir => () =>
  println(s"Trigger the next job: http://orkestra.company.com/orkestra/second?runId=${runId.value}")
}

lazy val secondJobBoard = JobBoard[() => Unit](JobId("second"), "Second")()
lazy val secondJob = Job(secondJobBoard) { implicit workDir => () =>
  println("Second job running")
}
```
