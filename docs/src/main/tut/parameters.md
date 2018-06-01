---
layout: docs
title:  "Parameters"
position: 3
---

# Parameters

If you'd like to pass parameters to a job you can add the corresponding UI elements, here is an example:
```tut:silent
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model.JobId
import com.goyeau.orkestra.parameter._

// Note that the signature of the function now contains the types of the parameters
lazy val parametersJobBoard = JobBoard[(String, Boolean) => Unit](JobId("parameters"), "Parameters")(
  Input[String]("Git ref"),
  Checkbox("Run tests?")
)
lazy val parametersJob = Job(parametersJobBoard) { implicit workDir => (gitRef, runTests) =>
  println(s"Building app for Git ref $gitRef${if (runTests) " and running tests" else ""}")
}
```

<img alt="Parameters" srcset="img/parameters.png 2x">

## Select

As you can see on the above screenshot it also supports drop-down lists via [Enumeratum](https://github.com/lloydmeta/enumeratum),
so let's try that out!  
First of all we need to add the [Enumeratum](https://github.com/lloydmeta/enumeratum) dependency in `build.sbt`:
```
// Not the triple %%% to import the cross compiled JS/JVM version
libraryDependencies += "com.beachape" %%% "enumeratum" % "Enumeratum version"
```

Then we can refine the previous code to include the drop-down:
```tut:silent
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model.JobId
import com.goyeau.orkestra.parameter._
import enumeratum._

// We declare the enum
sealed trait Environment extends EnumEntry
object Environment extends Enum[Environment] {
  case object Prod extends Environment
  case object Staging extends Environment
  case object QA extends Environment
  val values = findValues
}

lazy val parametersJobBoard = JobBoard[(String, Boolean, Environment) => Unit](JobId("parameters"), "Parameters")(
  Input[String]("Git ref"),
  Checkbox("Run tests?"),
  Select("Deploy on", Environment, Option(Environment.QA))
)
lazy val parametersJob = Job(parametersJobBoard) { implicit workDir => (gitRef, runTests, env) =>
  println(s"Building app from Git ref $gitRef${if (runTests) " and running tests" else ""} and deploying on $env")
}
```

## Input

Let's have a deeper look at the `Input`:  
`Input`s can also be used for other types like `Int` or `Double`, all you need to do is specifying the type you want to
use: `Input[Int]("Some Int")`.

We can also define default values: `Input[Double]("Some Double", default = Option(4.2))`.

It is a good practice to type as much as possible and therefore avoid generic types like `String`s. Orkestra is able to
handle any case class of one argument:
```tut:silent
import com.goyeau.orkestra._
import com.goyeau.orkestra.Dsl._
import com.goyeau.orkestra.board._
import com.goyeau.orkestra.job._
import com.goyeau.orkestra.model.JobId
import com.goyeau.orkestra.parameter._

// We create our better typed Git Ref
case class Ref(value: String) extends AnyVal

// Note that we use Ref instead of the generic String
lazy val parametersJobBoard = JobBoard[(Ref, Boolean) => Unit](JobId("parameters"), "Parameters")(
  Input[Ref]("Git ref"),
  Checkbox("Run tests?")
)
lazy val parametersJob = Job(parametersJobBoard) { implicit workDir => (gitRef, runTests) =>
  println(s"Building app from Git ref ${gitRef.value}${if (runTests) " and running tests" else ""}")
}
```
Here the text entered by the user will be wrapped into the `Ref` case class and given to the `Job` function.

## Checkbox

By default `Checkbox`s are not checked but you can make them checked: `Checkbox("Run tests?", checked = true)`
