package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.{AutowireServer, OrchestraConfig, RunInfo}
import io.circe.Json
import io.circe.generic.auto._

object ContainerUtils {
  private val homeDirMount = Json.obj(
    "mountPath" -> Json.fromString(OrchestraConfig.workspace),
    "name" -> Json.fromString("home")
  )
  private val homeDirVolume = Json.obj(
    "name" -> Json.fromString("home"),
    "emptyDir" -> Json.obj()
  )

  private def createContainer(container: Container, envs: Vector[Json], volumes: Vector[Json]) = Json.obj(
    "name" -> Json.fromString(container.name),
    "image" -> Json.fromString(container.image),
    "env" -> Json.fromValues(envs),
    "stdin" -> Json.True,
    "stdout" -> Json.True,
    "stderr" -> Json.True,
    "tty" -> Json.fromBoolean(container.tty),
    "command" -> Json.fromValues(container.command.map(Json.fromString)),
    "workingDir" -> Json.fromString(OrchestraConfig.workspace),
    "volumeMounts" -> Json.fromValues(volumes)
  )

  def createSpec(masterPod: Json, runInfo: RunInfo, podConfig: PodConfig[_]) = {
    val spec = masterPod.hcursor.downField("spec")
    val container = spec.downField("containers").downArray.first
    val envs = container.downField("env").focus.get.asArray.get :+ Json.obj(
      "name" -> Json.fromString("ORCHESTRA_RUN_INFO"),
      "value" -> Json.fromString(AutowireServer.write(runInfo))
    )
    val volumeMounts = container.downField("volumeMounts").values.toVector.flatten :+ ContainerUtils.homeDirMount
    val volumes = spec.downField("volumes").values.toVector.flatten :+ ContainerUtils.homeDirVolume

    Json.obj(
      "template" -> Json.obj(
        "spec" -> Json.obj(
          "containers" -> Json.arr(
            Json.obj(
              "name" -> container.downField("name").focus.get,
              "image" -> container.downField("image").focus.get,
              "env" -> Json.fromValues(envs),
              "workingDir" -> Json.fromString(OrchestraConfig.workspace),
              "volumeMounts" -> Json.fromValues(volumeMounts)
            ) +: podConfig.containerSeq.map(createContainer(_, envs, volumeMounts)): _*
          ),
          "volumes" -> Json.fromValues(volumes),
          "restartPolicy" -> Json.fromString("Never")
        )
      )
    )
  }
}
