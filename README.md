<img alt="Orkestra" src="https://raw.githubusercontent.com/orkestracd/orkestra/master/docs/src/main/resources/microsite/img/orkestra.png" srcset="https://raw.githubusercontent.com/orkestracd/orkestra/master/docs/src/main/resources/microsite/img/orkestra.png 2x">

[![Latest version](https://index.scala-lang.org/orkestracd/orkestra/orkestra-core/latest.svg?color=blue)](https://index.scala-lang.org/orkestracd/orkestra/orkestra-core)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/OrkestraCD/orkestra)

Orkestra is an Open Source Continuous Integration / Continuous Deployment server as a library running on
[Kubernetes](https://kubernetes.io).  
It leverages Kubernetes concepts such as Jobs or Secrets, and configuration as code in [Scala](https://scala-lang.org)
to take the most of compile time type safety and compatibility with Scala or Java libraries.

Key features:
* Configured completely via code which can be version controlled
* Fully scalable
* Highly Available
* Extendable via any Scala/Java libraries


# Quick start

## Requirements

- [Java](https://java.com/download)
- [SBT](https://scala-sbt.org)
- [Kubernetes](https://kubernetes.io) or [Minikube](https://github.com/kubernetes/minikube)

## Installation

*project/plugins.sbt*:
```scala
addSbtPlugin("com.goyeau" % "sbt-orkestra" % "<latest version>")
```
*build.sbt*:
```scala
lazy val orkestra = orkestraProject("orkestra", file("orkestra"))
  .settings(
    libraryDependencies ++= Seq(
      "com.goyeau" %%% "orkestra-github" % orkestraVersion, // Optional Github plugin
      "com.goyeau" %%% "orkestra-cron" % orkestraVersion, // Optional Cron plugin
      "com.goyeau" %% "orkestra-lock" % orkestraVersion // Optional Lock plugin
    )
  )
lazy val orkestraJVM = orkestra.jvm
lazy val orkestraJS = orkestra.js
```

## Simple example

Given the above [installation](#installation), here is a minimal project with one job:

*orkestra/src/main/scala/orkestra.scala*:
```tut:silent
import com.goyeau.orkestra._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model._

// We extend OrkestraServer to create the web server
object Orkestra extends OrkestraServer {
  // Configuring the UI
  lazy val board = deployFrontendJobBoard
  // Configuring the jobs
  lazy val jobs = Set(deployFrontendJob)
  
  // Creating the job and configuring UI related settings
  lazy val deployFrontendJobBoard = JobBoard[() => Unit](JobId("deployFrontend"), "Deploy Frontend")()
  // Creating the job from the above definition (this will be compiled to JVM)
  lazy val deployFrontendJob = Job(deployFrontendJobBoard) { implicit workDir => () =>
    println("Deploying Frontend")
  }
}
```
This example is described in [Jobs & Boards](https://orkestracd.github.io/orkestra/jobsboards.html).

[See example projects](https://github.com/orkestracd/orkestra/tree/master/examples)

## Deployment on Kubernetes with Minikube

We provide some basic Kubernetes Deployment in [kubernetes-dev](https://github.com/orkestracd/orkestra/tree/master/examples/kubernetes-dev)
that you can use to deploy on a dev environment.  
Assuming that you are in one of the [example projects](https://github.com/orkestracd/orkestra/tree/master/examples)
(or in your own project), here is how to deploy on Kubernetes with Minikube:
```
minikube start                            # Start Minikube
eval `minikube docker-env`                # Make docker use the docker engine of Minikube
sbt orkestraJVM/Docker/publishLocal  # Publish the docker artifact
kubectl apply -f ../kubernetes-dev        # Apply the deployement to Kubernetes
kubectl proxy                             # Proxy the Kubernetes api
```
Visit Orkestra on `httpe://127.0.0.1:8001/api/v1/namespaces/orkestra/services/orkestra:http/proxy`.  
You can troubleshoot any deployment issue with `minikube dashboard`.

More on how to configure the deployment in [Config](https://orkestracd.github.io/orkestra/config.html).

# Documentation

Find all the documentation on [https://orkestracd.github.io/orkestra/](https://orkestracd.github.io/orkestra/)
- [Jobs & Boards](https://orkestracd.github.io/orkestra/jobsboards.html)
- [Config](https://orkestracd.github.io/orkestra/config.html)
- [Parameters](https://orkestracd.github.io/orkestra/parameters.html)
- [Stages](https://orkestracd.github.io/orkestra/stages.html)
- [Shell scripts](https://orkestracd.github.io/orkestra/shells.html)
- [Directories](https://orkestracd.github.io/orkestra/directories.html)
- [Secrets](https://orkestracd.github.io/orkestra/secrets.html)
- [Triggering jobs](https://orkestracd.github.io/orkestra/triggers.html)
- [RunId](https://orkestracd.github.io/orkestra/runid.html)
- [Containers](https://orkestracd.github.io/orkestra/containers.html)
- [Plugins](https://orkestracd.github.io/orkestra/plugins/)


# Related projects

* [Jenkins](https://jenkins.io)
* [Kubernetes Plugin for Jenkins](https://github.com/jenkinsci/kubernetes-plugin)
