import microsites.ExtraMdFileConfig
import org.scalajs.sbtplugin.ScalaJSCrossVersion
import sbtcrossproject.{crossProject, CrossType}

lazy val orchestra = project
  .in(file("."))
  .aggregate(
    `orchestra-coreJVM`,
    `orchestra-coreJS`,
    `orchestra-githubJVM`,
    `orchestra-githubJS`,
    `orchestra-cronJVM`,
    `orchestra-cronJS`,
    `orchestra-lock`,
    `orchestra-plugin`,
    `orchestra-integration-testsJVM`,
    `orchestra-integration-testsJS`,
    docs
  )
  .settings(
    name := "Orchestra",
    ThisBuild / organization := "com.goyeau",
    ThisBuild / licenses += "APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0"),
    ThisBuild / homepage := Option(url("https://orchestracd.github.io/orchestra/")),
    ThisBuild / scmInfo := Option(
      ScmInfo(url("https://github.com/orchestracd/orchestra"), "https://github.com/orchestracd/orchestra.git")
    ),
    ThisBuild / developers += Developer(id = "joan38",
                                        name = "Joan Goyeau",
                                        email = "joan@goyeau.com",
                                        url = url("http://goyeau.com")),
    ThisBuild / scalaVersion := "2.12.4",
    ThisBuild / dynverSonatypeSnapshots := true,
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
      if (isSnapshot.value) Opts.resolver.sonatypeSnapshots
      else Opts.resolver.sonatypeStaging
    ),
    publishArtifact := false,
    publishLocal := {}
  )

/***************** Projects *****************/
lazy val `orchestra-core` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .enablePlugins(BuildInfoPlugin)
  .jsSettings(
    jsDependencies ++= Seq(
      "org.webjars.npm" % "ansi_up" % "2.0.2" / "ansi_up.js" commonJSName "ansi_up"
    ) ++ react.value
  )
  .settings(
    name := "Orchestra Core",
    buildInfoPackage := s"${organization.value}.orchestra",
    buildInfoKeys += "projectName" -> "Orchestra",
    resolvers += Opts.resolver.sonatypeSnapshots,
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.vmunier" %% "scalajs-scripts" % "1.1.2",
      "com.lihaoyi" %%% "autowire" % "0.2.6",
      "com.goyeau" %% "kubernetes-client" % "0.0.4"
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
lazy val `orchestra-coreJVM` = `orchestra-core`.jvm
lazy val `orchestra-coreJS` = `orchestra-core`.js

lazy val `orchestra-github` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orchestra-core` % CompileTest)
  .settings(
    name := "Orchestra Github",
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.9.0.201710071750-r"
  )
lazy val `orchestra-githubJVM` = `orchestra-github`.jvm
lazy val `orchestra-githubJS` = `orchestra-github`.js

lazy val `orchestra-cron` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orchestra-core` % CompileTest)
  .settings(name := "Orchestra Cron")
lazy val `orchestra-cronJVM` = `orchestra-cron`.jvm
lazy val `orchestra-cronJS` = `orchestra-cron`.js

lazy val `orchestra-lock` = project
  .dependsOn(`orchestra-coreJVM` % CompileTest)
  .settings(name := "Orchestra Lock")

lazy val `orchestra-plugin` = project
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "Orchestra Plugin",
    moduleName := "sbt-orchestra",
    sbtPlugin := true,
    buildInfoPackage := s"${organization.value}.orchestra",
    addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.4.0"),
    addSbtPlugin("org.scala-js" %% "sbt-scalajs" % "0.6.23"),
    addSbtPlugin("com.vmunier" %% "sbt-web-scalajs" % "1.0.7"),
    addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.4")
  )

lazy val docs = project
  .in(file("docs"))
  .dependsOn(`orchestra-coreJVM`, `orchestra-githubJVM`, `orchestra-cronJVM`, `orchestra-lock`)
  .enablePlugins(MicrositesPlugin)
  .settings(
    name := "Orchestra",
    description := "DevOps with Scala and Kubernetes",
    micrositeGithubOwner := "OrchestraCD",
    micrositeGithubRepo := "orchestra",
    micrositeBaseUrl := micrositeGithubRepo.value,
    micrositeHighlightTheme := "atom-one-light",
    micrositeGitterChannelUrl := "OrchestraCD/Orchestra",
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
    publishArtifact := false,
    publishLocal := {}
  )

lazy val `orchestra-integration-tests` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .dependsOn(`orchestra-core`, `orchestra-github`, `orchestra-cron`)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "Orchestra Integration Tests",
    version ~= (_.replace('+', '-')),
    buildInfoPackage := s"${organization.value}.orchestra.integration.tests",
    buildInfoKeys += "artifactName" -> artifact.value.name,
    libraryDependencies ++= scalaTest.value,
    publishArtifact := false,
    publishLocal := {}
  )
lazy val `orchestra-integration-testsJVM` = `orchestra-integration-tests`.jvm
  .dependsOn(`orchestra-lock`)
  .enablePlugins(JavaAppPackaging)
  .configs(TestCi)
  .settings(
    dockerUpdateLatest := true,
    Test / test := (Test / test).dependsOn(Docker / publishLocal).value,
    TestCi / test := (Test / test).dependsOn(Docker / publish).value
  )
lazy val `orchestra-integration-testsJS` = `orchestra-integration-tests`.js

lazy val TestCi = config("testci").extend(Test)
lazy val CompileTest = "compile->compile;test->test"

/*************** Dependencies ***************/
lazy val akkaHttp = Def.setting {
  val akkaHttpVersion = "10.1.1"
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
    "com.github.japgolly.scalacss" % "core" % scalaCssVersion cross ScalaJSCrossVersion.binary,
    "com.github.japgolly.scalacss" % "ext-react" % scalaCssVersion cross ScalaJSCrossVersion.binary
  )
}

lazy val scalaJsReact = Def.setting {
  val scalaJsReactVersion = "1.2.0"
  Seq(
    "com.github.japgolly.scalajs-react" % "core" % scalaJsReactVersion cross ScalaJSCrossVersion.binary,
    "com.github.japgolly.scalajs-react" % "extra" % scalaJsReactVersion cross ScalaJSCrossVersion.binary
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
