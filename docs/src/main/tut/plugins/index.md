---
layout: docs
title:  "Plugins"
position: 11
---

# Plugins

Orkestra doesn't have a plugins system like you can find in other CD tools with their own store. Instead it relies on
jar dependencies and distribution systems like Maven or Ivy.  
There are multiple advantages to this system:
- Since Scala is a JVM language we have access to all the libraries from the JVM community (Scala, Java or any other
JVM compiled languages).
- Installation of plugins becomes code too. So it's very easy to code review and rollback changes

## Talking to AWS

In order to talk to AWS we can use the [official Java SDK](https://github.com/aws/aws-sdk-java). Let's add the dependency
to `build.sbt`:
```scala
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "AWS SDK Version"
```

Now we can define a function `uploadToS3()` that we will be able to use in any job:
```tut:silent
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import tech.orkestra.filesystem._
import tech.orkestra.utils.BlockingShells._

// We need the implicit workDir in order to know in which directory we are working in
def uploadToS3()(implicit workDir: Directory): Unit = {
  val s3 = AmazonS3ClientBuilder.standard.withRegion(Regions.EU_WEST_1).build()
  val transferManager = TransferManagerBuilder.standard.withS3Client(s3).build()

  // Creating the file locally 
  val file = LocalFile("uploadme.txt")
  sh(s"echo 'Hey!' > ${file.getName}")

  val s3Bucket = "some-bucket-name"
  println(s"Uploading ${file.getName} to S3 bucket $s3Bucket")
  transferManager.upload(s3Bucket, file.getName, file).waitForCompletion()
}
```

## Sending a Slack message

Let's try to integrate with Slack. If I Google "slack scala" and hit "I'm feeling lucky" I end up on this Slack client
[https://github.com/gilbertw1/slack-scala-client](https://github.com/gilbertw1/slack-scala-client). So it seems that
someone already wrote a Slack plugin for Orkestra even before Orkestra was born!  
Let's add the dependency to `build.sbt`:
```scala
libraryDependencies += "com.github.gilbertw1" %% "slack-scala-client" % "Slack client version"
```

And create the `sendSlackMessage()` function:
```tut:silent
import slack.api.SlackApiClient
// Orkestra already uses Akka so we can import the implicits for the Slack too
import tech.orkestra.utils.AkkaImplicits._

def sendSlackMessage(): Unit = {
  val slack = SlackApiClient("slack token")
  slack.postChatMessage("channel name", "Hello!")
}
```
You will be able to get the token by creating a Slack app on https://api.slack.com/apps and then adding it to your
workspace.

Their are endless possibilities with the access to all the Java world and this is why Orkestra has been designed from
the beginning as code first.  
See also the [Github Integration](github.html), [Cron jobs](cron.html) and [Locking](github.html) documentation.
