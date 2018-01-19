import org.scalajs.sbtplugin.cross.CrossProject

lazy val orchestra = project
  .in(file("."))
  .aggregate(coreJVM, coreJS, github, cron, lock)
  .settings(
    name := "Orchestra",
    ThisBuild / organization := "io.chumps",
    ThisBuild / scalaVersion := "2.12.4",
    ThisBuild / version := {
      val ver = (ThisBuild / version).value
      if (ver.contains("+")) ver + "-SNAPSHOT"
      else ver
    },
    ThisBuild / scalacOptions ++= Seq("-deprecation",
                                      "-feature",
                                      "-Ywarn-unused:imports",
                                      "-Ypartial-unification",
                                      "-Ywarn-dead-code"),
    ThisBuild / publishTo := Option(
      "DriveTribe Private" at "s3://drivetribe-repositories.s3-eu-west-1.amazonaws.com/maven"
    ),
    publishArtifact := false,
    publishLocal := {}
  )

/***************** Projects *****************/
lazy val core = CrossProject("orchestra-core", file("orchestra-core"), CrossType.Pure)
  .enablePlugins(BuildInfoPlugin)
  .jsSettings(
    jsDependencies ++= Seq(
      "org.webjars.npm" % "ansi_up" % "2.0.2" / "ansi_up.js" commonJSName "ansi_up"
    ) ++ react.value
  )
  .settings(
    name := "Orchestra Core",
    buildInfoKeys ++= Seq("projectName" -> "Orchestra"),
    buildInfoPackage := s"${organization.value}.orchestra",
    resolvers += Opts.resolver.sonatypeSnapshots,
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.vmunier" %% "scalajs-scripts" % "1.1.1",
      "com.beachape" %%% "enumeratum" % "1.5.12" % Provided,
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "com.goyeau" %% "kubernetes-client" % "0.0.1+28-929842cd-SNAPSHOT"
    ) ++ scalaJsReact.value ++ akkaHttp.value ++ scalaCss.value ++ logging.value ++ circe.value ++ elastic4s.value
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val github = Project("orchestra-github", file("orchestra-github"))
  .dependsOn(coreJVM % Provided)
  .settings(
    name := "Orchestra Github",
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.9.0.201710071750-r"
  )

lazy val cron = Project("orchestra-cron", file("orchestra-cron"))
  .dependsOn(coreJVM % Provided)
  .settings(name := "Orchestra Cron")

lazy val lock = Project("orchestra-lock", file("orchestra-lock"))
  .dependsOn(coreJVM % Provided)
  .settings(name := "Orchestra Lock")

/*************** Dependencies ***************/
lazy val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.0.11"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )
}

lazy val logging = Def.setting {
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}

lazy val scalaCss = Def.setting {
  val scalaCssVersion = "0.5.3"
  Seq(
    "com.github.japgolly.scalacss" %%%! "core" % scalaCssVersion,
    "com.github.japgolly.scalacss" %%%! "ext-react" % scalaCssVersion
  )
}

lazy val scalaJsReact = Def.setting {
  val scalaJsReactVersion = "1.1.1"
  Seq(
    "com.github.japgolly.scalajs-react" %%%! "core" % scalaJsReactVersion,
    "com.github.japgolly.scalajs-react" %%%! "extra" % scalaJsReactVersion
  )
}

lazy val react = Def.setting {
  val reactVersion = "15.6.1"
  Seq(
    "org.webjars.bower" % "react" % reactVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
    "org.webjars.bower" % "react" % reactVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
  )
}

lazy val circe = Def.setting {
  val version = "0.9.0"
  Seq(
    "io.circe" %%% "circe-core" % version,
    "io.circe" %%% "circe-generic" % version,
    "io.circe" %%% "circe-parser" % version,
    "io.circe" %%% "circe-shapes" % version,
    "io.circe" %%% "circe-java8" % version
  )
}

lazy val elastic4s = Def.setting {
  val elastic4sVersion = "6.1.1"
  Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-circe" % elastic4sVersion
  )
}
