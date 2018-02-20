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

```scala
"io.chumps" %%% "orchestra-core" % "<Latest version>"
```
Opional plugins:
```
"io.chumps" %%% "orchestra-github" % "<Latest version>"
"io.chumps" %%% "orchestra-cron" % "<Latest version>"
"io.chumps" %% "orchestra-lock" % "<Latest version>"
```

See example

## Usage

TBD


## Related projects

* [Jenkins](https://jenkins.io)
* [Kubernetes Plugin for Jenkins](https://github.com/jenkinsci/kubernetes-plugin)
