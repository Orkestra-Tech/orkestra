package com.drivetribe.orchestra

import java.io.File

import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.{CrossProject, CrossType}
import sbt.Keys._
import sbt._
import webscalajs.ScalaJSWeb
import webscalajs.WebScalaJS.autoImport._

object OrchestraPlugin extends AutoPlugin {
  object autoImport {
    val orchestraVersion = BuildInfo.version

    object OrchestraProject {
      def apply(id: String, base: File): CrossProject = {
        val cross = CrossProject(id, base, CrossType.Pure)
        cross
          .jvmConfigure(_.enablePlugins(SbtWeb, JavaAppPackaging))
          .jvmSettings(
            Assets / WebKeys.packagePrefix := "public/",
            Runtime / managedClasspath += (Assets / packageBin).value,
            Assets / pipelineStages := Seq(scalaJSPipeline),
            scalaJSProjects := Seq(cross.js)
          )
          .jsConfigure(_.enablePlugins(ScalaJSPlugin, ScalaJSWeb))
          .jsSettings(scalaJSUseMainModuleInitializer := true, moduleName := "web")
          .settings(libraryDependencies += "com.drivetribe" %%% "orchestra-core" % orchestraVersion)
      }
    }
  }
}
