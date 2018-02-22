Orchestra
=========

Orchestra is an Open Source Continuous Integration / Continuous Deployment server as a library running on Kubernetes.  
It leverages Kubernetes concepts such as Jobs or Secrets, and configuration as code in Scala to take the most of compile time type safety and compatibility with Scala or Java libraries.


Key features:
* Configured completely via code which can be version controlled
* Fully scalable
* Highly Available
* Extendable via any Scala/Java libraries


## Installation

*project/plugins.sbt*:
```scala
addSbtPlugin("io.chumps" % "sbt-orchestra" % "<latest version>")
```
*build.sbt*:
```scala
lazy val orchestration = OrchestraProject("orchestration", file("orchestration"))
  .settings(
    libraryDependencies ++= Seq(
      "io.chumps" %%% "orchestra-github" % orchestraVersion, // Optional Github plugin
      "io.chumps" %%% "orchestra-cron" % orchestraVersion, // Optional Cron plugin
      "io.chumps" %% "orchestra-lock" % orchestraVersion // Optional Lock plugin
    )
  )
lazy val orchestrationJVM = orchestration.jvm
lazy val orchestrationJS = orchestration.js
```

[See example projects](examples)


## Usage

Given the above [installation](#installation), here is a minimal project with one Hello World job:

*orchestration/src/main/scala/Orchestration.scala*:
```scala
import io.chumps.orchestra.AsyncDsl._
import io.chumps.orchestra.{Orchestra, UI}
import io.chumps.orchestra.board.{Folder, Job}
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.JobId

object Orchestration extends Orchestra with UI {
  lazy val board = Folder("My amazing company")(helloJob)
  lazy val jobRunners = Set(helloJobRunner)
  
  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  lazy val helloJobRunner = JobRunner(job) { implicit workDir => () =>
    println("Hello World")
  }
}
```

[See example projects](examples)


## Deployment on Kubernetes with Minikube

Assuming that you are in one of the [example projects](examples) (or in your own project), here is how to deploy on Kubernetes with Minikube:
```
minikube start                            # Start Minikube
eval `minikube docker-env`                # Make docker use the docker engin of Minikube
sbt orchestrationJVM/Docker/publishLocal  # Publish the docker artifact
kubectl apply -f ../kubernetes-dev        # Apply the deployement to Kubernetes
kubectl proxy                             # Proxy the Kubernetes api
```
Visit Orchestra on `https://127.0.0.1:8001/api/v1/namespaces/orchestra/services/orchestration:http/proxy`.  
You can troubleshoot any deployment issue with `minikube dashboard`.


## Related projects

* [Jenkins](https://jenkins.io)
* [Kubernetes Plugin for Jenkins](https://github.com/jenkinsci/kubernetes-plugin)
