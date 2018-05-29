---
layout: docs
title:  "Directories"
position: 6
---

# Directories

When playing with files we often need to change directories, so you might wonder if we do that in Orchestra!  
First of all this is now time to explain what the `implicit workDir`. This implicit is used for example by the function
`sh()` as seen in [Shell scripts](shells.html) to know where to run the shell script.  
So if we'd like to change directory we would use the function `dir` that changes the current directory only for the
scope of the function passed to it:
```tut:silent
import scala.concurrent.Await
import scala.concurrent.duration._
import com.goyeau.orchestra._
import com.goyeau.orchestra.Dsl._
import com.goyeau.orchestra.board._
// We import the directories DSL
import com.goyeau.orchestra.utils.Directories._
import com.goyeau.orchestra.job._
import com.goyeau.orchestra.model._
import com.goyeau.orchestra.utils.Shells._

lazy val directoryJobBoard = JobBoard[() => Unit](JobId("directory"), "Directory")()
lazy val directoryJob = Job(directoryJobBoard) { implicit workDir => () =>
  Await.result(for {
    // Create the directory subDir
    _ <- sh("mkdir subDir")
    // Moving in the subDir
    _ <- dir("subDir") { implicit workDir =>
      // Print the working directory
      sh("pwd")
    }
  } yield (), 1.second)
}
```
Note that we need to take this `implicit workDir =>` again in the function passed to `dir`. 

## LocalFile

When dealing with files in Scala we usually use the `java.io.File`. In Orchestra as we can change the current working
directory we need the relative `File` to be aware of it. This is why we have the `LocalFile` class that extends `File`
so that we can use it in Scala or Java libraries.  
Here is an example of how to use it:
```tut:silent
import com.goyeau.orchestra.filesystem._

def createFile()(implicit workDir: Directory): Unit = { 
  dir("subDir") { implicit workDir =>
    LocalFile("myFile").createNewFile()
  }
}
```
