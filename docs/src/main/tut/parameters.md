---
layout: docs
title:  "Parameters"
position: 3
---

# Parameters

If you'd like to pass parameters to a job you can add the corresponding UI elements, here is an example:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId
import com.drivetribe.orchestra.parameter._

// Note that the signature of the function now contains the types of the parameters
lazy val parametersJob = Job[(String, Boolean) => Unit](JobId("parameters"), "Parameters")(
  Input[String]("Git ref"),
  Checkbox("Run tests?")
)
lazy val parametersJobRunner = JobRunner(parametersJob) { implicit workDir => (gitRef, runTests) =>
  println(s"Building app for Git ref $gitRef${if (runTests) " and running tests" else ""}")
}
```

<img alt="Parameters" srcset="img/parameters.png 2x">

As you can see on the screenshot it also supports drop-down lists via [Enumeratum](https://github.com/lloydmeta/enumeratum),
so let's add this drop-down.  
First of all we need to add the [Enumeratum](https://github.com/lloydmeta/enumeratum) dependency in `build.sbt`:
```
// Not the triple %%% to import the cross compiled JS/JVM version
libraryDependencies += "com.beachape" %%% "enumeratum" % "Enumeratum version"
```

Then we can refine the previous code to include the drop-down:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
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

`Input`s are also used for other types like `Int` or `Double`, all you need to do is `Input[Int]("Some Int")`.  
We can also define default value `Input[Double]("Some Double", default = Option(4.2))`.

## Typed Input
It is a good practice to type as much as possible and therefore avoid generic types like `String`s. Orchestra is able to
handle any case class of one argument:
```tut:silent
import com.drivetribe.orchestra._
import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId
import com.drivetribe.orchestra.parameter._

// We create our better typed Git Ref
case class Ref(value: String) extends AnyVal

// Note that we use Ref instead of the generic String
lazy val parametersJob = Job[(Ref, Boolean) => Unit](JobId("parameters"), "Parameters")(
  Input[Ref]("Git ref"),
  Checkbox("Run tests?")
)
lazy val parametersJobRunner = JobRunner(parametersJob) { implicit workDir => (gitRef, runTests) =>
  println(s"Building app from Git ref ${gitRef.value}${if (runTests) " and running tests" else ""}")
}
```
