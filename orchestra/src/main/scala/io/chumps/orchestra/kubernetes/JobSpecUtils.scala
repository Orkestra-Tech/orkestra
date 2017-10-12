package io.chumps.orchestra.kubernetes

import io.chumps.orchestra.{AutowireServer, OrchestraConfig}
import scala.language.reflectiveCalls

import io.circe.Encoder
import io.circe.generic.auto._
import io.k8s.api.batch.v1.JobSpec
import io.k8s.api.core.v1._

object JobSpecUtils {
  private val home = "home"
  private val homeDirMount = VolumeMount(home, mountPath = OrchestraConfig.workspace)
  private val homeDirVolume = Volume(home, emptyDir = Option(EmptyDirVolumeSource()))

  private val downwardApi = "downward-api"
  private val downwardApiMount = VolumeMount(downwardApi, mountPath = OrchestraConfig.downwardApi.toString)
  private val downwardApiVolume = Volume(
    downwardApi,
    downwardAPI = Option(
      DownwardAPIVolumeSource(
        items = Option(Seq(DownwardAPIVolumeFile("labels", fieldRef = Option(ObjectFieldSelector("metadata.labels")))))
      )
    )
  )

  private def createContainer(container: Container, masterContainer: Container): Container =
    container.copy(
      stdin = Option(true),
      env = Option((container.env ++ masterContainer.env).flatten.toSeq),
      workingDir = container.workingDir.orElse(Option(OrchestraConfig.workspace)),
      volumeMounts = Option(
        distinctOnName(
          (container.volumeMounts ++ masterContainer.volumeMounts).flatten.toSeq :+ homeDirMount
        )
      )
    )

  def createJobSpec[RunInfo: Encoder](masterPod: Pod, runInfo: RunInfo, podConfig: PodConfig[_]) = {
    val masterSpec = masterPod.spec.get
    val masterContainer = masterSpec.containers.head
    val runInfoEnvVar = EnvVar("ORCHESTRA_RUN_INFO", value = Option(AutowireServer.write(runInfo)))
    val slaveContainer = masterContainer.copy(
      env = Option(distinctOnName(runInfoEnvVar +: masterContainer.env.toSeq.flatten)),
      workingDir = Option(OrchestraConfig.workspace),
      volumeMounts =
        Option(distinctOnName(masterContainer.volumeMounts.toSeq.flatten :+ homeDirMount :+ downwardApiMount))
    )

    JobSpec(
      template = PodTemplateSpec(
        spec = Option(
          PodSpec(
            nodeSelector = Option(podConfig.nodeSelector),
            containers = slaveContainer +: podConfig.containerSeq.map(createContainer(_, masterContainer)),
            volumes = Option(
              distinctOnName(
                podConfig.volumes ++ masterSpec.volumes.toSeq.flatten :+ homeDirVolume :+ downwardApiVolume
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
