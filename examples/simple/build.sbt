lazy val orkestra = orkestraProject("orkestra", file("orkestra"))
  .jvmSettings(scalaJSPipeline / devCommands ++= Seq("publishLocal", "console", "test"))
