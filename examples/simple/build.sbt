lazy val orchestration = orchestraProject("orchestration", file("orchestration"))
lazy val orchestrationJVM = orchestration.jvm
  .settings(scalaJSPipeline / devCommands ++= Seq("publishLocal", "console", "test"))
lazy val orchestrationJS = orchestration.js
