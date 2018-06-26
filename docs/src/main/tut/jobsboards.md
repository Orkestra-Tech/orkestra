---
layout: docs
title:  "Jobs & Boards"
position: 1
---

# Jobs & Boards

Orkestra is split in 2 main parts, the `Board`s which are UI elements and actual `Job`s that contains the tasks to
execute and how to execute them.

## The Orkestra trait

The trait `Orkestra` act as the main of our app, it will start the web server.  
Mixing in `Orkestra` requires us to implement 2 attributes, `board: Board` that will be the root Board to
display and `jobs: Set[Job]` that will be our set of jobs that can be executed by Orkestra:
```tut:silent
import tech.orkestra._

object Orkestra extends OrkestraServer {
  // Configuring the UI
  lazy val board = ???

  // Configuring the jobs
  lazy val jobs = ???
}
```

## Boards

The boards are UI elements, there is 2 main implementation of `Board`: `JobBoard` and `Folder`.

### JobBoard
`JobBoard` represent the job on the UI:
```tut:silent
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.model._

JobBoard[() => Unit](JobId("deployFrontend"), "Deploy Frontend")()
```
- `() => Unit` The function this job will be executing. Here it will have no parameter and not return anything.
- `JobId("deployFrontend")` The JobId is a unique identifier for the job.
- `"Deploy Frontend"` We give it a pretty name for the display.
- `()` This job has no parameters here so we give empty brackets. This will be described in [Parameters](parameters.html).

### Folder
`Folder`s let you create a tree that can eventually contain `JobBoard`s: 
```tut:silent
import tech.orkestra.board._
import tech.orkestra.model._

Folder("Orkestra")(
  Folder("Some folder")(
    JobBoard[() => Unit](JobId("someId1"), "Some Job 1")()
  ),
  JobBoard[() => Unit](JobId("someId2"), "Some Job 2")()
)
```

## Job

Now that we have the UI defined we can define the actual `Job` with a function to run when the user hit the "Run"
button on the UI.  
Here is a full example with a `Folder`, the `JobBoard` and the `Job`:
```tut:silent
import tech.orkestra._
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.job._
import tech.orkestra.model._

object Orkestra extends OrkestraServer {
  lazy val board = Folder("Orkestra")(deployFrontendJobBoard)
  lazy val jobs = Set(deployFrontendJob)

  lazy val deployFrontendJobBoard = JobBoard[() => Unit](JobId("deployFrontend"), "Deploy Frontend")()
  lazy val deployFrontendJob = Job(deployFrontendJobBoard) { implicit workDir => () =>
    println("Deploying Frontend")
  }
}
```
- `deployFrontendJobBoard` Attach the board (UI) to this Job.
- `implicit workDir` Defines the working directory. This will be described in [Directories](directories.html).
- `() => println("Deploying Frontend")` The function to execute when the job is ran.
