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

[See example projects](examples)


## Usage

Given the above [installation](#installation), here is a minimal project with one Hello World job:

*orchestration/src/main/scala/Orchestration.scala*:
```scala
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.{Orchestra, UI}
import com.drivetribe.orchestra.board.{Folder, Job}
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId

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


## Licence

```
This software is licensed under the Apache License, version 2 ("ALv2"), quoted below.

Copyright 2018 Tribe IP Limited 

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
```
