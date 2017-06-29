package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.{AutowireServer, OrchestraConfig, RunInfo}
import io.circe.Json
import io.circe.generic.auto._

object JobUtils {
  private val workspace = s"/opt/docker/${OrchestraConfig.workspace.getPath}"
  private val homeDirMount = Json.obj(
    "mountPath" -> Json.fromString(workspace),
    "name" -> Json.fromString("home")
  )
  private val homeDirVolume = Json.obj(
    "name" -> Json.fromString("home"),
    "emptyDir" -> Json.obj()
  )

  private def createContainer(container: Container) = Json.obj(
    "name" -> Json.fromString(container.name),
    "image" -> Json.fromString(container.image),
    "stdin" -> Json.True,
    "stdout" -> Json.True,
    "stderr" -> Json.True,
    "tty" -> Json.fromBoolean(container.tty),
    "command" -> Json.fromValues(container.command.map(Json.fromString)),
    "workingDir" -> Json.fromString(workspace),
    "volumeMounts" -> Json.arr(homeDirMount)
  )

  def createSpec(masterPod: Json, runInfo: RunInfo, podConfig: PodConfig[_]) = {
    val spec = masterPod.hcursor.downField("spec")
    val container = spec.downField("containers").downArray.first
    val envs = container.downField("env").focus.get.asArray.get :+ Json.obj(
      "name" -> Json.fromString("ORCHESTRA_RUN_INFO"),
      "value" -> Json.fromString(AutowireServer.write(runInfo))
    )

    Json.obj(
      "template" -> Json.obj(
        "spec" -> Json.obj(
          "containers" -> Json.arr(
            Json.obj(
              "name" -> container.downField("name").focus.get,
              "image" -> container.downField("image").focus.get,
              "env" -> Json.fromValues(envs),
              "volumeMounts" -> Json.arr(
                container.downField("volumeMounts").values.toVector.flatten :+ JobUtils.homeDirMount: _*
              )
            ) +: podConfig.containerSeq.map(createContainer): _*
          ),
          "volumes" -> Json.arr(spec.downField("volumes").values.toVector.flatten :+ JobUtils.homeDirVolume: _*),
          "restartPolicy" -> Json.fromString("Never")
        )
      )
    )
  }
}
