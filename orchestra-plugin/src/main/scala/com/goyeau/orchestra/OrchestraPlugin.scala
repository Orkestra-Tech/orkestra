package com.goyeau.orchestra

import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import webscalajs.ScalaJSWeb
import webscalajs.WebScalaJS.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbtcrossproject.{CrossProject, CrossType}
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport.{toScalaJSGroupID => _, _}

object OrchestraPlugin extends AutoPlugin {
  object autoImport {
    val orchestraVersion = BuildInfo.version

    def orchestraProject(id: String, base: File): CrossProject = {
      val crossProject = CrossProject(id, base)(JVMPlatform, JSPlatform).crossType(CrossType.Pure)
      crossProject
        .jvmConfigure(_.enablePlugins(SbtWeb, JavaAppPackaging))
        .jvmSettings(
          Assets / WebKeys.packagePrefix := "public/",
          Runtime / managedClasspath += (Assets / packageBin).value,
          Assets / pipelineStages := Seq(scalaJSPipeline),
          scalaJSProjects := Seq(crossProject.js)
        )
        .jsConfigure(_.enablePlugins(ScalaJSWeb))
        .jsSettings(scalaJSUseMainModuleInitializer := true, moduleName := "web")
        .settings(libraryDependencies += "com.goyeau" %%% "orchestra-core" % orchestraVersion)
    }
  }
}
