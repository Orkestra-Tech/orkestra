---
layout: docs
title:  "Jobs & Boards"
position: 1
---

# Jobs & Boards

Orchestra is split in 2 main parts, the `Board`s which are UI elements and actual `Job`s that contains the tasks to
execute and how to execute them.

## The Orchestra trait

The trait `Orchestra` act as the main of our app, it will start the web server.  
Mixing in `Orchestra` requires us to implement 2 attributes, `board: Board` that will be the root Board to
display and `jobs: Set[Job]` that will be our set of jobs that can be executed by Orchestra:
```tut:silent
import com.goyeau.orchestra._

object Orchestration extends Orchestra {
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
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.model._

JobBoard[() => Unit](JobId("deployFrontend"), "Deploy Frontend")()
```
- `() => Unit` The function this job will be executing. Here it will have no parameter and not return anything.
- `JobId("deployFrontend")` The JobId is a unique identifier for the job.
- `"Deploy Frontend"` We give it a pretty name for the display.
- `()` This job has no parameters here so we give empty brackets. This will be described in [Parameters](parameters.html).

### Folder
`Folder`s let you create a tree that can eventually contain `JobBoard`s: 
```tut:silent
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.model._

Folder("Orchestra")(
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
import com.goyeau.orchestra._
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._

object Orchestration extends Orchestra {
  lazy val board = Folder("Orchestra")(deployFrontendJobBoard)
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
