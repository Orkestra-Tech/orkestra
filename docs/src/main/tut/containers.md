---
layout: docs
title:  "Containers"
position: 9
---

# Containers

The Orkestra jobs have a one to one mapping to [Kubernetes Jobs](https://kubernetes.io/docs/concepts/workloads/controllers/jobs-run-to-completion/).
That means when you run a job, Orkestra will actually schedule a Kubernetes job that will schedule a [Pod](https://kubernetes.io/docs/concepts/workloads/pods/pod/)
that will run a container where the code of your job will be executed.

As you may already know it is possible to run multiple containers in a Pod and Orkestra provides a way to do so from
the `Job` configuration.  
We might want to run multiple containers per job for example to run a database that we will be used while running tests
for a pull request, or if you need to run a command like `terraform apply` or `sbt test`.

Let's write a job that will run `sbt test` assuming that the tests need to have Elasticsearch available on localhost:
```tut:silent
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.job._
import tech.orkestra.model._
// We import the shells DSL
import tech.orkestra.utils.BlockingShells._
import io.k8s.api.core.v1.{Container, EnvVar, PodSpec}

lazy val testJobBoard = JobBoard[() => Unit](JobId("test"), "Test")()

// Configure containers for SBT and Elasticsearch
val sbt = Container(
  name = "sbt",
  image = Option("hseeberger/scala-sbt"),
  tty = Option(true), // TTY needs to be enabled if you want to run shell scripts in this container
  command = Option(Seq("cat")) // We override the command to an infinitly blocking command that does nothing 
)
val elasticsearch = Container(
  name = "elasticsearch",
  image = Option("docker.elastic.co/elasticsearch/elasticsearch-oss"),
  env = Option(Seq(EnvVar(name = "cluster.name", value = Option("test"))))
)

// We pass a PodSpec with the containers' configuration 
lazy val testJob = Job(testJobBoard)(PodSpec(Seq(sbt, elasticsearch))) { implicit workDir => () =>
  // Note that we run the shell in the SBT container
  sh("sbt test", sbt)
}
```
