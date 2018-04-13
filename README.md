<img alt="Orchestra" srcset="https://raw.githubusercontent.com/drivetribe/orchestra/master/docs/src/main/resources/microsite/img/orchestra.png 2x">

[![Latest version](https://index.scala-lang.org/drivetribe/orchestra/orchestra-core/latest.svg?color=blue)](https://index.scala-lang.org/drivetribe/orchestra/orchestra-core)

Orchestra is an Open Source Continuous Integration / Continuous Deployment server as a library running on Kubernetes.  
It leverages Kubernetes concepts such as Jobs or Secrets, and configuration as code in Scala to take the most of compile
time type safety and compatibility with Scala or Java libraries.

Key features:
* Configured completely via code which can be version controlled
* Fully scalable
* Highly Available
* Extendable via any Scala/Java libraries


# Quick start

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

## Hello World example

Given the above [installation](#installation), here is a minimal project with one Hello World job:

*orchestration/src/main/scala/Orchestration.scala*:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.AsyncDsl._
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
  // - () => Unit          This job will have no parameter and return nothing
  // - JobId("helloWorld") The JobId is a unique identifier
  // - "Hello World"       And we give it a pretty name
  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  // Creating the job runner from the above definition (this will be compiled to JVM)
  // - helloJob         This JobRunner will be registered for the Job helloJob
  // - implicit workDir Defines the working directory, we will not be using it in this example
  lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
    println("Hello World")
  }
}
```

[See example projects](https://github.com/drivetribe/orchestra/tree/master/examples)

## Deployment on Kubernetes with Minikube

Assuming that you are in one of the [example projects](https://github.com/drivetribe/orchestra/tree/master/examples)
(or in your own project), here is how to deploy on Kubernetes with Minikube:
```
minikube start                            # Start Minikube
eval `minikube docker-env`                # Make docker use the docker engine of Minikube
sbt orchestrationJVM/Docker/publishLocal  # Publish the docker artifact
kubectl apply -f ../kubernetes-dev        # Apply the deployement to Kubernetes
kubectl proxy                             # Proxy the Kubernetes api
```
Visit Orchestra on `https://127.0.0.1:8001/api/v1/namespaces/orchestra/services/orchestration:http/proxy`.  
You can troubleshoot any deployment issue with `minikube dashboard`.


# Documentation

Find all the documentation on [https://drivetribe.github.io/orchestra/](https://drivetribe.github.io/orchestra/)


# Related projects

* [Jenkins](https://jenkins.io)
* [Kubernetes Plugin for Jenkins](https://github.com/jenkinsci/kubernetes-plugin)
