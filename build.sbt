version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.2"
scalacOptions in ThisBuild += "-deprecation"

val scalaJsReact = Def.setting {
  Seq(
    "com.github.japgolly.scalajs-react" %%% "core",
    "com.github.japgolly.scalajs-react" %%% "extra"
  ).map(_ % "1.0.0")
}

val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.0.5"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "de.heikoseeberger" %% "akka-http-circe" % "1.15.0"
  )
}

val scalaCss = Def.setting {
  Seq(
    "com.github.japgolly.scalacss" %%% "core",
    "com.github.japgolly.scalacss" %%% "ext-react"
  ).map(_ % "0.5.3")
}

val react = Def.setting {
  val reactVersion = "15.4.2"
  Seq(
    "org.webjars.bower" % "react" % reactVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % reactVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
  )
}

val autowire = Def.setting(Seq("com.lihaoyi" %%% "autowire" % "0.2.6"))

val circe = Def.setting {
  Seq(
    "io.circe" %%% "circe-core",
    "io.circe" %%% "circe-generic",
    "io.circe" %%% "circe-parser"
  ).map(_ % "0.7.0")
}

lazy val orchestra = crossProject
  .crossType(CrossType.Pure)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.2",
      "com.vmunier" %% "scalajs-scripts" % "1.1.0"
    ) ++ scalaJsReact.value ++ circe.value ++ akkaHttp.value ++ scalaCss.value ++ autowire.value,
    jsDependencies ++= react.value
  )
lazy val orchestraJVM = orchestra.jvm
lazy val orchestraJS = orchestra.js

// Exemple
lazy val orchestration = crossProject
  .crossType(CrossType.Pure)
  .dependsOn(orchestra)

lazy val orchestrationJVM = orchestration.jvm
  .enablePlugins(SbtWeb)
  .settings(
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    scalaJSProjects := Seq(orchestrationJS),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )
lazy val orchestrationJS = orchestration.js
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    moduleName in fastOptJS := "web",
    moduleName in fullOptJS := "web",
    moduleName in packageJSDependencies := "web",
    moduleName in packageMinifiedJSDependencies := "web"
  )
