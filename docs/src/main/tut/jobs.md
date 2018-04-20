---
layout: docs
title:  "Jobs"
position: 1
---

# Jobs

```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

// We extend Orchestra to create the API server and UI to create the web UI
object Orchestration extends Orchestra with UI {
  // Configuring the UI
  lazy val board = Folder("Orchestra")(helloJob)
  // Configuring the job runners
  lazy val jobRunners = Set(helloJobRunner)
  
  // Defining the job and configuring UI related settings (this will be mostly compiled to JS)
  // - () => Unit           This job will have no parameter and return nothing
  // - JobId("helloWorld")  The JobId is a unique identifier for the job
  // - "Hello World"        And we give it a pretty name for the display
  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  // Creating the job runner from the above definition (this will be compiled to JVM)
  // - helloJob                      This JobRunner will be registered for the Job helloJob
  // - implicit workDir              Defines the working directory, we will not be using it in this example
  // - () => println("Hello World")  The function to execute when the job is ran
  lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
    println("Hello World")
  }
}
```
