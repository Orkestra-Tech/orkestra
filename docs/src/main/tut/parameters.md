---
layout: docs
title:  "Parameters"
position: 1
---

# Parameters

If you'd like to pass parameters to a job you can add the corresponding UI elements, here is an example:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId
import com.drivetribe.orchestra.parameter._

// Note that the signature of the function now contains the types of the parameters
lazy val parametersJob = Job[(String, Boolean) => Unit](JobId("parameters"), "Parameters")(Input[String]("Git ref"),
                                                                                           Checkbox("Run tests?"))
lazy val parametersJobRunner = JobRunner(parametersJob) { implicit workDir => (gitRef, runTests) =>
  println(s"Building app for Git ref $gitRef${if (runTests) " and running tests" else ""}")
}
```

Here is an example of the UI with the parameters:  
<img alt="Parameters" srcset="img/parameters.png 2x">

As you can see on the screenshot it also supports drop-down lists via [Enumeratum](https://github.com/lloydmeta/enumeratum),
so let's refine the previous code to include the drop-down:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId
import com.drivetribe.orchestra.parameter._
import enumeratum._

// We declare the enum
sealed trait Environment extends EnumEntry
object Environment extends Enum[Environment] {
  case object Prod extends Environment
  case object Staging extends Environment
  case object QA extends Environment
  val values = findValues
}

lazy val parametersJob = Job[(String, Boolean, Environment) => Unit](JobId("parameters"), "Parameters")(
  Input[String]("Git ref"),
  Checkbox("Run tests?"),
  Select("Deploy on", Environment, Option(Environment.QA))
)
lazy val parametersJobRunner = JobRunner(parametersJob) { implicit workDir => (gitRef, runTests, env) =>
  println(s"Building app from Git ref $gitRef${if (runTests) " and running tests" else ""} and deploying on $env")
}
```
