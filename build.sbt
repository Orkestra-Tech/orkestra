import microsites.ExtraMdFileConfig
import org.scalajs.sbtplugin.ScalaJSCrossVersion
import sbtcrossproject.{crossProject, CrossType}

lazy val orkestra = project
  .in(file("."))
  .aggregate(
    `orkestra-coreJVM`,
    `orkestra-coreJS`,
    `orkestra-githubJVM`,
    `orkestra-githubJS`,
    `orkestra-cronJVM`,
    `orkestra-cronJS`,
    `orkestra-lock`,
    `orkestra-sbt`,
    `orkestra-integration-testsJVM`,
    `orkestra-integration-testsJS`,
    docs
  )
  .settings(
    name := "Orkestra",
    ThisBuild / organization := "tech.orkestra",
    ThisBuild / licenses += "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    ThisBuild / homepage := Option(url("https://orkestra.tech")),
    ThisBuild / scmInfo := Option(
      ScmInfo(url("https://github.com/orkestra-tech/orkestra"), "https://github.com/orkestra-tech/orkestra.git")
    ),
    ThisBuild / developers += Developer(
      id = "joan38",
      name = "Joan Goyeau",
      email = "joan@goyeau.com",
      url = url("http://goyeau.com")
    ),
    ThisBuild / scalaVersion := "2.12.4",
    Global / releaseEarlyWith := SonatypePublisher,
    Global / releaseEarlyEnableLocalReleases := true,
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
    publishArtifact := false,
    publishLocal := {}
  )

/***************** Projects *****************/
lazy val `orkestra-core` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(BuildInfoPlugin)
  .jsSettings(
    jsDependencies ++= Seq(
      ("org.webjars.npm" % "ansi_up" % "2.0.2" / "ansi_up.js").commonJSName("ansi_up")
    ) ++ react.value
  )
  .settings(
    name := "Orkestra Core",
    buildInfoPackage := organization.value,
    buildInfoKeys += "projectName" -> "Orkestra",
    resolvers += Opts.resolver.sonatypeSnapshots,
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.vmunier" %% "scalajs-scripts" % "1.1.2",
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "com.goyeau" %% "kubernetes-client" % "0.0.5"
    ) ++
      scalaJsReact.value ++
      akkaHttp.value ++
      akkaHttpCirce.value ++
      circe.value ++
      scalaCss.value ++
      logging.value ++
      elastic4s.value ++
      scalaTest.value ++
      enumeratum.value.map(_ % Provided)
  )
lazy val `orkestra-coreJVM` = `orkestra-core`.jvm
lazy val `orkestra-coreJS` = `orkestra-core`.js

lazy val `orkestra-github` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orkestra-core` % CompileTest)
  .settings(
    name := "Orkestra Github",
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.9.0.201710071750-r"
  )
lazy val `orkestra-githubJVM` = `orkestra-github`.jvm
lazy val `orkestra-githubJS` = `orkestra-github`.js

lazy val `orkestra-cron` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orkestra-core` % CompileTest)
  .settings(name := "Orkestra Cron")
lazy val `orkestra-cronJVM` = `orkestra-cron`.jvm
lazy val `orkestra-cronJS` = `orkestra-cron`.js

lazy val `orkestra-lock` = project
  .dependsOn(`orkestra-coreJVM` % CompileTest)
  .settings(name := "Orkestra Lock")

lazy val `orkestra-sbt` = project
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "Orkestra Plugin",
    moduleName := "sbt-orkestra",
    sbtPlugin := true,
    buildInfoPackage := organization.value,
    addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.4.0"),
    addSbtPlugin("org.scala-js" %% "sbt-scalajs" % "0.6.23"),
    addSbtPlugin("com.vmunier" %% "sbt-web-scalajs" % "1.0.7"),
    addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.4")
  )

lazy val docs = project
  .in(file("docs"))
  .dependsOn(`orkestra-coreJVM`, `orkestra-githubJVM`, `orkestra-cronJVM`, `orkestra-lock`)
  .enablePlugins(MicrositesPlugin)
  .settings(
    name := "Orkestra",
    description := "Functional DevOps with Scala and Kubernetes",
    micrositeGithubOwner := "Orkestra-Tech",
    micrositeGithubRepo := "orkestra",
    micrositeHighlightTheme := "atom-one-light",
    micrositeGitterChannelUrl := "orkestra-tech/Orkestra",
    micrositePalette ++= Map(
      "brand-primary" -> "#DA3435",
      "brand-secondary" -> "#3570E5",
      "brand-tertiary" -> "#3570E5"
    ),
    micrositeExtraMdFiles += file("README.md") -> ExtraMdFileConfig("index.md", "home"),
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk" % "1.11.310",
      "com.github.gilbertw1" %% "slack-scala-client" % "0.2.3",
      "org.zeroturnaround" % "zt-zip" % "1.12"
    ) ++ enumeratum.value,
    git.remoteRepo := "https://github.com/orkestra-tech/orkestra-tech.github.io.git",
    ghpagesBranch := "master",
    publishArtifact := false,
    publishLocal := {}
  )

lazy val `orkestra-integration-tests` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orkestra-core`, `orkestra-github`, `orkestra-cron`)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "Orkestra Integration Tests",
    version ~= (_.replace('+', '-')),
    buildInfoPackage := s"${organization.value}.integration.tests",
    buildInfoKeys += "artifactName" -> artifact.value.name,
    libraryDependencies ++= scalaTest.value,
    publishArtifact := false,
    publishLocal := {}
  )
lazy val `orkestra-integration-testsJVM` = `orkestra-integration-tests`.jvm
  .dependsOn(`orkestra-lock`)
  .enablePlugins(JavaAppPackaging)
  .configs(TestCi)
  .settings(
    dockerUpdateLatest := true,
    Test / test := (Test / test).dependsOn(Docker / publishLocal).value,
    TestCi / test := (Test / test).dependsOn(Docker / publish).value
  )
lazy val `orkestra-integration-testsJS` = `orkestra-integration-tests`.js

lazy val TestCi = config("testci").extend(Test)
lazy val CompileTest = "compile->compile;test->test"

/*************** Dependencies ***************/
lazy val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.1.3"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream" % "2.5.11",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  )
}

lazy val akkaHttpCirce = Def.setting {
  Seq("de.heikoseeberger" %% "akka-http-circe" % "1.20.1")
}

lazy val circe = Def.setting {
  val version = "0.9.3"
  Seq(
    "io.circe" %%% "circe-core" % version,
    "io.circe" %%% "circe-generic" % version,
    "io.circe" %%% "circe-parser" % version,
    "io.circe" %%% "circe-shapes" % version,
    "io.circe" %%% "circe-java8" % version
  )
}

lazy val logging = Def.setting {
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
}

lazy val scalaCss = Def.setting {
  val scalaCssVersion = "0.5.5"
  Seq(
    ("com.github.japgolly.scalacss" % "core" % scalaCssVersion).cross(ScalaJSCrossVersion.binary),
    ("com.github.japgolly.scalacss" % "ext-react" % scalaCssVersion).cross(ScalaJSCrossVersion.binary)
  )
}

lazy val scalaJsReact = Def.setting {
  val scalaJsReactVersion = "1.2.3"
  Seq(
    ("com.github.japgolly.scalajs-react" % "core" % scalaJsReactVersion).cross(ScalaJSCrossVersion.binary),
    ("com.github.japgolly.scalajs-react" % "extra" % scalaJsReactVersion).cross(ScalaJSCrossVersion.binary)
  )
}

lazy val react = Def.setting {
  val reactVersion = "16.2.0"
  Seq(
    ("org.webjars.npm" % "react" % reactVersion / "umd/react.development.js")
      .minified("umd/react.production.min.js")
      .commonJSName("React"),
    ("org.webjars.npm" % "react-dom" % reactVersion / "umd/react-dom.development.js")
      .minified("umd/react-dom.production.min.js")
      .dependsOn("umd/react.development.js")
      .commonJSName("ReactDOM"),
    ("org.webjars.npm" % "react-dom" % reactVersion / "umd/react-dom-server.browser.development.js")
      .minified("umd/react-dom-server.browser.production.min.js")
      .dependsOn("umd/react-dom.development.js")
      .commonJSName("ReactDOMServer")
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
  Seq("org.scalatest" %% "scalatest" % "3.0.5" % Test)
}

lazy val enumeratum = Def.setting {
  Seq("com.beachape" %%% "enumeratum" % "1.5.13")
}
