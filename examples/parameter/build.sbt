lazy val orkestra = orkestraProject("orkestra", file("orkestra"))
lazy val orkestraJVM = orkestra.jvm
  .settings(scalaJSPipeline / devCommands ++= Seq("publishLocal", "console", "test"))
lazy val orkestraJS = orkestra.js

ThisBuild / dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2"
