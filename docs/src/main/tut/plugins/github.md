---
layout: docs
title:  "Github"
---

# Github Integration

Orchestra has a Github integration via the `orchestra-github` library/plugin. We can do so by adding the dependency in
`build.sbt`:
```scala
libraryDependencies += "com.goyeau" %%% "orchestra-github" % orchestraVersion
```

To add a webhook server so that Github can trigger jobs, we mix in the trait `GithubHooks`, which requires us to
implement `githubTriggers: Set[GithubTrigger]`. There is two implementation of `GithubTrigger`:
 - `BranchTrigger` which let's you trigger a job when a change is done on a branch.
 - `PullRequestTrigger` to listen on pull request changes.

Let's have a look first to `BranchTrigger`:
```tut:silent
import com.goyeau.orchestra._
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
// We import the Github package
import com.goyeau.orchestra.github._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
import com.goyeau.orchestra.parameter._

object Orchestration extends Orchestra with GithubHooks { // Note that we mix in GithubHooks
  lazy val board = Folder("Orchestra")(branchJobBoard)
  lazy val jobs = Set(branchJob) // We still need to add the Job to jobs
  
  // We add the BranchTrigger to the githubTriggers
  lazy val githubTriggers = Set(BranchTrigger(Repository("myOrganisation/myRepo"), "master", branchJob)())

  lazy val branchJobBoard = JobBoard[GitRef => Unit](JobId("branch"), "Branch")(Input[GitRef]("Git ref"))
  lazy val branchJob = Job(branchJobBoard) { implicit workDir => gitRef =>
    println("Hello World")
  }
}
```

Before we jump into the `PullRequestTrigger` I'd like to introduce `Github.statusUpdated`, a little utility function
that checkouts the Git ref and run the code updating the status on Github for the Git ref
(<img alt="Github pending" srcset="img/github-pending.png 2x"><img alt="Github success" srcset="img/github-success.png 2x"><img alt="Github failure" srcset="img/github-failure.png 2x">)
according to if an exception has been thrown in the code:
```tut:silent
import com.goyeau.orchestra._
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
// We import the Github package
import com.goyeau.orchestra.github._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
import com.goyeau.orchestra.parameter._

object Orchestration extends Orchestra with GithubHooks { // Note that we mix in GithubHooks
  lazy val board = Folder("Orchestra")(pullRequestJobBoard)
  lazy val jobs = Set(pullRequestJob) // We still need to add the Job to jobs

  // We add the PullRequestTrigger to the githubTriggers 
  lazy val githubTriggers = Set(PullRequestTrigger(Repository("myOrganisation/myRepo"), pullRequestJob)())

  lazy val pullRequestJobBoard = JobBoard[GitRef => Unit](JobId("pullRequest"), "Pull Request")(
    Input[GitRef]("Git ref")
  )
  lazy val pullRequestJob = Job(pullRequestJobBoard) { implicit workDir => gitRef =>
    Github.statusUpdated(Repository("myOrganisation/myRepo"), gitRef) { implicit workDir =>
      println("This PR is good of course")
    }
  }
}
```

## Config

- `ORCHESTRA_GITHUB_URI`: The URI of the home page as Github displays links to it for PRs for example. Required.
- `ORCHESTRA_GITHUB_TOKEN`: The token of the account that will be used to clone or to update commit statuses. Required.
- `ORCHESTRA_GITHUB_PORT`: The separate port where to bind the Github Hooks server. Default `8081`.

The reason why the Github hooks server is bound on separate port is so that you can make Orchestra accessible only
internally but still expose the Github hooks server on the public network.  

Once we have the hooks setup in Orchestra we need Github to call them. We can do so in the settings of the repository
(`https://github.com/<org>/<repo>/settings/hooks`) where we will `Add webhook` as the following:
- `Payload URL`: `http://<host>/webhooks`
- `Content type`: `application/json`
- `Send me everything.`
