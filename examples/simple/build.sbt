lazy val orkestra = orkestraProject("orkestra", file("orkestra"))
lazy val orkestraJVM = orkestra.jvm
  .settings(scalaJSPipeline / devCommands ++= Seq("publishLocal", "console", "test"))
lazy val orkestraJS = orkestra.js
