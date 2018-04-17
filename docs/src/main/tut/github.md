---
layout: docs
title:  "Github"
position: 10
---

# Github Integration

Orchestra has a Github integration via the `orchestra-github` library/plugin. We can do so by adding the dependency in
`build.sbt`:
```scala
libraryDependencies += "com.drivetribe" %%% "orchestra-github" % orchestraVersion
```

To add a webhook server so that Github can trigger jobs we mix in the trait `GithubHooks`, which requires us to
implement `githubTriggers: Set[GithubTrigger]`. There is two implementation of `GithubTrigger`:
 - `BranchTrigger` which let's you trigger a job when a change is done on a branch.
 - `PullRequestTrigger` to listen on pull request changes.

Let's have a look first to `BranchTrigger`:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.github._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
import com.drivetribe.orchestra.parameter._

object Orchestration extends Orchestra with UI with GithubHooks { // Note we mix in GithubHooks
  lazy val board = Folder("Orchestra")(branchJob)
  lazy val jobRunners = Set(branchJobRunner) // We still need to add the job runners to the jobRunners
  
  // We add the BranchTrigger to the githubTriggers
  lazy val githubTriggers = Set(BranchTrigger(Repository("myOrganisation/myRepo"), "master", branchJobRunner)())

  lazy val branchJob = Job[GitRef => Unit](JobId("branch"), "Branch")(Input[GitRef]("Git ref"))
  lazy val branchJobRunner = JobRunner(branchJob) { implicit workDir => gitRef =>
    println("Hello World")
  }
}
```

Before we jump into the `PullRequestTrigger` I'd like to introduce `Github.statusUpdated`, a little utility function
that checkouts the Git ref and run the code updating the status on Github for the Git ref
(<img alt="Github pending" srcset="img/github-pending.png 2x"><img alt="Github success" srcset="img/github-success.png 2x"><img alt="Github failure" srcset="img/github-failure.png 2x">)
according to if an exception has been thrown in the code:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.github._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._
import com.drivetribe.orchestra.parameter._

object Orchestration extends Orchestra with UI with GithubHooks { // Note we mix in GithubHooks
  lazy val board = Folder("Orchestra")(pullRequestJob)
  lazy val jobRunners = Set(pullRequestJobRunner) // We still need to add the job runners to the jobRunners

  // We add the PullRequestTrigger to the githubTriggers 
  lazy val githubTriggers = Set(PullRequestTrigger(Repository("myOrganisation/myRepo"), pullRequestJobRunner)())

  lazy val pullRequestJob = Job[GitRef => Unit](JobId("pullRequest"), "Pull Request")(Input[GitRef]("Git ref"))
  lazy val pullRequestJobRunner = JobRunner(pullRequestJob) { implicit workDir => gitRef =>
    Github.statusUpdated(Repository("myOrganisation/myRepo"), gitRef) { implicit workDir =>
      println("This PR is good of course")
    }
  }
}
```
