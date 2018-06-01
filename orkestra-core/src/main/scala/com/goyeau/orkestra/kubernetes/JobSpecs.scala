package com.goyeau.orkestra.kubernetes

import com.goyeau.orkestra.OrkestraConfig
import scala.language.reflectiveCalls

import io.circe.generic.auto._
import io.circe.syntax._
import io.k8s.api.batch.v1.JobSpec
import io.k8s.api.core.v1._

import com.goyeau.orkestra.model.EnvRunInfo

private[orkestra] object JobSpecs {
  private val home = "home"
  private def homeDirMount(implicit orkestraConfig: OrkestraConfig) =
    VolumeMount(home, mountPath = orkestraConfig.workspace)
  private val homeDirVolume = Volume(home, emptyDir = Option(EmptyDirVolumeSource()))

  private val downwardApi = "downward-api"
  private val downwardApiMount = VolumeMount(downwardApi, mountPath = OrkestraConfig.downwardApi.toString)
  private val downwardApiVolume = Volume(
    downwardApi,
    downwardAPI = Option(
      DownwardAPIVolumeSource(
        items = Option(Seq(DownwardAPIVolumeFile("labels", fieldRef = Option(ObjectFieldSelector("metadata.labels")))))
      )
    )
  )

  private def createContainer(container: Container, masterContainer: Container)(
    implicit orkestraConfig: OrkestraConfig
  ): Container =
    container.copy(
      stdin = Option(true),
      env = Option((container.env ++ masterContainer.env).flatten.toSeq),
      envFrom = Option((container.envFrom ++ masterContainer.envFrom).flatten.toSeq),
      workingDir = container.workingDir.orElse(Option(orkestraConfig.workspace)),
      volumeMounts = Option(
        distinctOnName(
          homeDirMount +: (container.volumeMounts ++ masterContainer.volumeMounts).flatten.toSeq
        )
      )
    )

  def create(masterPod: Pod, runInfo: EnvRunInfo, podSpec: PodSpec)(implicit orkestraConfig: OrkestraConfig) = {
    val masterSpec = masterPod.spec.get
    val masterContainer = masterSpec.containers.head
    val runInfoEnvVar = EnvVar("ORKESTRA_RUN_INFO", value = Option(runInfo.asJson.noSpaces))
    val slaveContainer = masterContainer.copy(
      env = Option(distinctOnName(runInfoEnvVar +: masterContainer.env.toSeq.flatten)),
      workingDir = Option(orkestraConfig.workspace),
      volumeMounts =
        Option(distinctOnName(homeDirMount +: downwardApiMount +: masterContainer.volumeMounts.toSeq.flatten))
    )

    JobSpec(
      template = PodTemplateSpec(
        spec = Option(
          podSpec.copy(
            slaveContainer +: podSpec.containers.map(createContainer(_, masterContainer)),
            volumes = Option(
              distinctOnName(
                homeDirVolume +: downwardApiVolume +: (podSpec.volumes.toSeq.flatten ++ masterSpec.volumes.toSeq.flatten)
              )
            ),
            restartPolicy = Option("Never")
          )
        )
      )
    )
  }

  private def distinctOnName[A <: { def name: String }](list: Seq[A]): Seq[A] =
    list.groupBy(_.name).values.map(_.head).toSeq
}
