---
layout: docs
title:  "Directories"
position: 5
---

# Directories

When playing with files we often need to change directories:
```tut:silent
import scala.concurrent.Await
import scala.concurrent.duration._
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
// We import the directories DSL
import com.drivetribe.orchestra.filesystem.Directories._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
import com.drivetribe.orchestra.utils.Shells._

lazy val directoryJob = Job[() => Unit](JobId("directory"), "Directory")()
lazy val directoryJobRunner = JobRunner(directoryJob) { implicit workDir => () =>
  Await.result(for {
    // Create the directory subDir
    _ <- sh("mkdir subDir")
    // Moving in the subDir
    _ <- dir("subDir") { implicit workDir =>
      // Print the working directory
      sh("pwd")
    }
  } yield (), 1.second)
}
```
