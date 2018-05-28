<img alt="Orchestra" src="https://raw.githubusercontent.com/drivetribe/orchestra/master/docs/src/main/resources/microsite/img/orchestra.png" srcset="https://raw.githubusercontent.com/drivetribe/orchestra/master/docs/src/main/resources/microsite/img/orchestra.png 2x">

[![Latest version](https://index.scala-lang.org/drivetribe/orchestra/orchestra-core/latest.svg?color=blue)](https://index.scala-lang.org/drivetribe/orchestra/orchestra-core)

Orchestra is an Open Source Continuous Integration / Continuous Deployment server as a library running on
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
addSbtPlugin("com.drivetribe" % "sbt-orchestra" % "<latest version>")
```
*build.sbt*:
```scala
lazy val orchestration = OrchestraProject("orchestration", file("orchestration"))
  .settings(
    libraryDependencies ++= Seq(
      "com.drivetribe" %%% "orchestra-github" % orchestraVersion, // Optional Github plugin
      "com.drivetribe" %%% "orchestra-cron" % orchestraVersion, // Optional Cron plugin
      "com.drivetribe" %% "orchestra-lock" % orchestraVersion // Optional Lock plugin
    )
  )
lazy val orchestrationJVM = orchestration.jvm
lazy val orchestrationJS = orchestration.js
```

## Simple example

Given the above [installation](#installation), here is a minimal project with one job:

*orchestration/src/main/scala/Orchestration.scala*:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job._
import com.drivetribe.orchestra.model._

// We extend Orchestra to create the web server
object Orchestration extends Orchestra {
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
This example is described in [Jobs & Boards](https://drivetribe.github.io/orchestra/jobsboards.html).

[See example projects](https://github.com/drivetribe/orchestra/tree/master/examples)

## Deployment on Kubernetes with Minikube

We provide some basic Kubernetes Deployment in [kubernetes-dev](https://github.com/drivetribe/orchestra/tree/master/examples/kubernetes-dev)
that you can use to deploy on a dev environment.  
Assuming that you are in one of the [example projects](https://github.com/drivetribe/orchestra/tree/master/examples)
(or in your own project), here is how to deploy on Kubernetes with Minikube:
```
minikube start                            # Start Minikube
eval `minikube docker-env`                # Make docker use the docker engine of Minikube
sbt orchestrationJVM/Docker/publishLocal  # Publish the docker artifact
kubectl apply -f ../kubernetes-dev        # Apply the deployement to Kubernetes
kubectl proxy                             # Proxy the Kubernetes api
```
Visit Orchestra on `httpe://127.0.0.1:8001/api/v1/namespaces/orchestra/services/orchestration:http/proxy`.  
You can troubleshoot any deployment issue with `minikube dashboard`.

More on how to configure the deployment in [Config](https://drivetribe.github.io/orchestra/config.html).

# Documentation

Find all the documentation on [https://drivetribe.github.io/orchestra/](https://drivetribe.github.io/orchestra/)
- [Jobs & Boards](https://drivetribe.github.io/orchestra/jobsboards.html)
- [Config](https://drivetribe.github.io/orchestra/config.html)
- [Parameters](https://drivetribe.github.io/orchestra/parameters.html)
- [Stages](https://drivetribe.github.io/orchestra/stages.html)
- [Shell scripts](https://drivetribe.github.io/orchestra/shells.html)
- [Directories](https://drivetribe.github.io/orchestra/directories.html)
- [Secrets](https://drivetribe.github.io/orchestra/secrets.html)
- [Triggering jobs](https://drivetribe.github.io/orchestra/triggers.html)
- [RunId](https://drivetribe.github.io/orchestra/runid.html)
- [Containers](https://drivetribe.github.io/orchestra/containers.html)
- [Plugins](https://drivetribe.github.io/orchestra/plugins/)


# Related projects

* [Jenkins](https://jenkins.io)
* [Kubernetes Plugin for Jenkins](https://github.com/jenkinsci/kubernetes-plugin)
