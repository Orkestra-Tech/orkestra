import org.scalajs.sbtplugin.cross.CrossProject

lazy val orchestra = project
  .in(file("."))
  .aggregate(coreJVM, coreJS, githubJVM, githubJS, cronJVM, cronJS, lock, plugin)
  .settings(
    name := "Orchestra",
    ThisBuild / organization := "io.chumps",
    ThisBuild / licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    ThisBuild / scalaVersion := "2.12.4",
    ThisBuild / version := {
      val ver = (ThisBuild / version).value
      if (ver.contains("+")) ver + "-SNAPSHOT"
      else ver
    },
    ThisBuild / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xlint:unsound-match",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
      "-Ypartial-unification",
      "-Ywarn-dead-code"
    ),
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
    buildInfoPackage := s"${organization.value}.orchestra",
    buildInfoKeys ++= Seq("projectName" -> "Orchestra"),
    resolvers ++= Seq(
      Opts.resolver.sonatypeSnapshots,
      "btomala at bintray" at "https://dl.bintray.com/btomala/maven/"
    ),
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.vmunier" %% "scalajs-scripts" % "1.1.1",
      "com.beachape" %%% "enumeratum" % "1.5.12" % Provided,
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "com.goyeau" %% "kubernetes-client" % "0.0.4"
    ) ++ scalaJsReact.value ++ akkaHttp.value ++ scalaCss.value ++ logging.value ++ circe.value ++ elastic4s.value ++ scalaTest.value
  )
lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val github = CrossProject("orchestra-github", file("orchestra-github"), CrossType.Pure)
  .dependsOn(core % Provided)
  .settings(
    name := "Orchestra Github",
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.9.0.201710071750-r"
  )
lazy val githubJVM = github.jvm
lazy val githubJS = github.js

lazy val cron = CrossProject("orchestra-cron", file("orchestra-cron"), CrossType.Pure)
  .dependsOn(core % Provided)
  .settings(name := "Orchestra Cron")
lazy val cronJVM = cron.jvm
lazy val cronJS = cron.js

lazy val lock = Project("orchestra-lock", file("orchestra-lock"))
  .dependsOn(coreJVM % Provided)
  .settings(name := "Orchestra Lock")

lazy val plugin = Project("orchestra-plugin", file("orchestra-plugin"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "Orchestra Plugin",
    moduleName := "sbt-orchestra",
    sbtPlugin := true,
    buildInfoPackage := s"${organization.value}.orchestra",
    addSbtPlugin("org.scala-js" %% "sbt-scalajs" % "0.6.21"),
    addSbtPlugin("com.vmunier" %% "sbt-web-scalajs" % "1.0.6"),
    addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.3")
  )

lazy val TestCi = config("testci").extend(Test)
lazy val integrationTests =
  CrossProject("orchestra-integration-tests", file("orchestra-integration-tests"), CrossType.Pure)
    .dependsOn(core, github, cron)
    .enablePlugins(BuildInfoPlugin)
    .settings(
      name := "Orchestra Integration Tests",
      version ~= (_.replace('+', '-')),
      buildInfoPackage := s"${organization.value}.orchestra.integration.tests",
      buildInfoKeys ++= Seq("artifactName" -> artifact.value.name),
      libraryDependencies ++= scalaTest.value
    )
lazy val integrationTestsJVM = integrationTests.jvm
  .dependsOn(lock)
  .enablePlugins(JavaAppPackaging)
  .configs(TestCi)
  .settings(
    dockerUpdateLatest := true,
    // Workaround the fact that ENTRYPOINT is not absolute, so when we change the WORKDIR it won't start
    dockerEntrypoint := Seq(s"${(Docker / defaultLinuxInstallLocation).value}/bin/${executableScriptName.value}"),
    dockerRepository := Option("registry.drivetribe.com/tools"),
    Test / test := (Test / test).dependsOn(Docker / publishLocal).value,
    TestCi / test := (Test / test).dependsOn(Docker / publish).value
  )
lazy val integrationTestsJS = integrationTests.js

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
  val scalaCssVersion = "0.5.5"
  Seq(
    "com.github.japgolly.scalacss" %%%! "core" % scalaCssVersion,
    "com.github.japgolly.scalacss" %%%! "ext-react" % scalaCssVersion
  )
}

lazy val scalaJsReact = Def.setting {
  val scalaJsReactVersion = "1.2.0"
  Seq(
    "com.github.japgolly.scalajs-react" %%%! "core" % scalaJsReactVersion,
    "com.github.japgolly.scalajs-react" %%%! "extra" % scalaJsReactVersion
  )
}

lazy val react = Def.setting {
  val reactVersion = "16.2.0"
  Seq(
    "org.webjars.npm" % "react" % reactVersion / "umd/react.development.js" minified "umd/react.production.min.js" commonJSName "React",
    "org.webjars.npm" % "react-dom" % reactVersion / "umd/react-dom.development.js" minified "umd/react-dom.production.min.js" dependsOn "umd/react.development.js" commonJSName "ReactDOM",
    "org.webjars.npm" % "react-dom" % reactVersion / "umd/react-dom-server.browser.development.js" minified "umd/react-dom-server.browser.production.min.js" dependsOn "umd/react-dom.development.js" commonJSName "ReactDOMServer"
  )
}

lazy val circe = Def.setting {
  val version = "0.9.1"
  Seq(
    "io.circe" %%% "circe-core" % version,
    "io.circe" %%% "circe-generic" % version,
    "io.circe" %%% "circe-parser" % version,
    "io.circe" %%% "circe-shapes" % version,
    "io.circe" %%% "circe-java8" % version
  )
}

lazy val elastic4s = Def.setting {
  val elastic4sVersion = "6.2.3"
  Seq(
    "com.sksamuel.elastic4s" %% "elastic4s-http" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-circe" % elastic4sVersion,
    "com.sksamuel.elastic4s" %% "elastic4s-testkit" % elastic4sVersion % Test
  )
}

lazy val scalaTest = Def.setting {
  Seq("org.scalatest" %% "scalatest" % "3.0.4" % Test)
}
