version in ThisBuild := "0.1"
scalaVersion in ThisBuild := "2.12.2"
scalacOptions in ThisBuild += "-deprecation"

val scalaJsReact = Def.setting {
  val scalaJsReactVersion = "1.1.0"
  Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % scalaJsReactVersion,
    "com.github.japgolly.scalajs-react" %%% "extra" % scalaJsReactVersion
  )
}

val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.0.9"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )
}

val logging = Def.setting {
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}

val scalaCss = Def.setting {
  val scalaCssVersion = "0.5.3"
  Seq(
    "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
    "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion
  )
}

val react = Def.setting {
  val reactVersion = "15.6.1"
  Seq(
    "org.webjars.bower" % "react" % reactVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % reactVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
  )
}

val autowire = Def.setting(Seq("com.lihaoyi" %%% "autowire" % "0.2.6"))

val circeVersion = "0.8.0"
val circeJS = Def.setting {
  Seq(
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "io.circe" %%% "circe-shapes" % circeVersion
  )
}
val circeJVM = Def.setting {
  Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-shapes" % circeVersion
  )
}

lazy val orchestra = crossProject
  .crossType(CrossType.Pure)
  .enablePlugins(ScalaJSPlugin)
  // For some reason we need to manually separate the JVM and JS version
  .jsSettings(libraryDependencies ++= circeJS.value)
  .jvmSettings(libraryDependencies ++= circeJVM.value)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.2",
      "com.vmunier" %% "scalajs-scripts" % "1.1.1",
      "org.eclipse.jgit" % "org.eclipse.jgit" % "4.8.0.201706111038-r"
    ) ++ scalaJsReact.value ++ akkaHttp.value ++ scalaCss.value ++ autowire.value ++ logging.value,
    jsDependencies ++= Seq(
      "org.webjars.npm" % "ansi_up" % "2.0.2" / "ansi_up.js" commonJSName "ansi_up"
    ) ++ react.value
  )
lazy val orchestraJVM = orchestra.jvm
lazy val orchestraJS = orchestra.js

// Exemple
lazy val orchestration = crossProject
  .crossType(CrossType.Pure)
  .dependsOn(orchestra)
  .settings(libraryDependencies += "com.beachape" %%% "enumeratum" % "1.5.12")

lazy val orchestrationJVM = orchestration.jvm
  .enablePlugins(SbtWeb, JavaAppPackaging)
  .settings(
    reForkOptions := reForkOptions.value.copy(
      envVars = reForkOptions.value.envVars ++ Map(
        "ORCHESTRA_HOME" -> "target/orchestra",
        "ORCHESTRA_PORT" -> "8080",
        "ORCHESTRA_GITHUB_PORT" -> "8081",
        "ORCHESTRA_KUBE_URI" -> "http://127.0.0.1:8001"
      )
    ),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    scalaJSProjects := Seq(orchestrationJS),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    maintainer in Docker := "github:joan38",
    dockerRepository := Option("registry.drivetribe.com/tools"),
    dockerUpdateLatest := true,
    dockerExposedPorts := Seq(8080, 8081),
    // Workaround the face that ENTRYPOINT is not absolute, so when we change the WORKDIR the won't start
    dockerEntrypoint := Seq(s"${(defaultLinuxInstallLocation in Docker).value}/bin/${executableScriptName.value}"),
    daemonUser in Docker := "root" // Workaround minikube volume rights
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
