---
layout: docs
title:  "Github"
---

# Github Integration

Orkestra has a Github integration via the `orkestra-github` library/plugin. We can do so by adding the dependency in
`build.sbt`:
```scala
libraryDependencies += "com.goyeau" %%% "orkestra-github" % orkestraVersion
```

To add a webhook server so that Github can trigger jobs, we mix in the trait `GithubHooks`, which requires us to
implement `githubTriggers: Set[GithubTrigger]`. There is two implementation of `GithubTrigger`:
 - `BranchTrigger` which let's you trigger a job when a change is done on a branch.
 - `PullRequestTrigger` to listen on pull request changes.

Let's have a look first to `BranchTrigger`:
```tut:silent
import com.goyeau.orkestra._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
// We import the Github package
import com.goyeau.orkestra.github._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._
import com.goyeau.orkestra.parameter._

object Orkestra extends OrkestraServer with GithubHooks { // Note that we mix in GithubHooks
  lazy val board = Folder("Orkestra")(branchJobBoard)
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
import com.goyeau.orkestra._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
// We import the Github package
import com.goyeau.orkestra.github._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._
import com.goyeau.orkestra.parameter._

object Orkestra extends OrkestraServer with GithubHooks { // Note that we mix in GithubHooks
  lazy val board = Folder("Orkestra")(pullRequestJobBoard)
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

- `ORKESTRA_GITHUB_URI`: The URI of the home page as Github displays links to it for PRs for example. Required.
- `ORKESTRA_GITHUB_TOKEN`: The token of the account that will be used to clone or to update commit statuses. Required.
- `ORKESTRA_GITHUB_PORT`: The separate port where to bind the Github Hooks server. Default `8081`.

The reason why the Github hooks server is bound on separate port is so that you can make Orkestra accessible only
internally but still expose the Github hooks server on the public network.  

Once we have the hooks setup in Orkestra we need Github to call them. We can do so in the settings of the repository
(`https://github.com/<org>/<repo>/settings/hooks`) where we will `Add webhook` as the following:
- `Payload URL`: `http://<host>/webhooks`
- `Content type`: `application/json`
- `Send me everything.`
