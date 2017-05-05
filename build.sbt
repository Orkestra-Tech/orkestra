val scalaJsReact = Def.setting {
  val scalaJSReactVersion = "1.0.0"
  Seq(
    "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
    "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion
  )
}
val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.0.5"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "de.heikoseeberger" %% "akka-http-circe" % "1.15.0"
  )
}

val autowire = Def.setting("com.lihaoyi" %%% "autowire" % "0.2.6")

val circe = Def.setting {
  Seq(
    "io.circe" %%% "circe-core",
    "io.circe" %%% "circe-generic",
    "io.circe" %%% "circe-parser"
  ).map(_ % "0.7.0")
}

lazy val common = crossProject
  .crossType(CrossType.Pure)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.2",
      autowire.value
    ) ++ scalaJsReact.value ++ circe.value ++ akkaHttp.value
  )
lazy val commonJVM = common.jvm
lazy val commonJS = common.js

lazy val backend = project
  .dependsOn(commonJVM)
  .enablePlugins(SbtWeb, SbtTwirl)
  .settings(
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.0"
    ),
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    scalaJSProjects := Seq(web),
    pipelineStages in Assets := Seq(scalaJSPipeline)
  )

lazy val web = project
  .dependsOn(commonJS)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= {
      val scalaCssVersion = "0.5.3"
      Seq(
        "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
        "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion
      )
    },
    jsDependencies ++= {
      val reactVersion = "15.4.2"
      Seq(
        "org.webjars.bower" % "react" % reactVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
        "org.webjars.bower" % "react" % reactVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
      )
    }
  )

lazy val orchestra = project
  .in(file("."))
  .settings(
    version in ThisBuild := "0.1",
    scalaVersion in ThisBuild := "2.12.2",
    scalacOptions in ThisBuild += "-deprecation"
  )
  .aggregate(backend, web)
