package io.chumps.orchestra.kubernetes

import io.chumps.orchestra.{AutowireServer, OrchestraConfig, RunInfo}
import scala.language.reflectiveCalls

import io.circe.generic.auto._
import io.k8s.api.batch.v1.JobSpec
import io.k8s.api.core.v1._

object JobSpecUtils {
  private val homeDirMount = VolumeMount(name = "home", mountPath = OrchestraConfig.workspace)
  private val homeDirVolume = Volume(name = "home", emptyDir = Option(EmptyDirVolumeSource()))

  private def createContainer(container: Container, masterContainer: Container): Container =
    container.copy(
      stdin = Option(true),
      env = Option((container.env ++ masterContainer.env).flatten.toSeq),
      workingDir = container.workingDir.orElse(Option(OrchestraConfig.workspace)),
      volumeMounts = Option(
        distinctOnName(
          (container.volumeMounts ++ masterContainer.volumeMounts).flatten.toSeq :+ JobSpecUtils.homeDirMount
        )
      )
    )

  def createJobSpec(masterPod: Pod, runInfo: RunInfo, podConfig: PodConfig[_]) = {
    val masterSpec = masterPod.spec.get
    val masterContainer = masterSpec.containers.head
    val runInfoEnvVar = EnvVar("ORCHESTRA_RUN_INFO", value = Option(AutowireServer.write(runInfo)))
    val slaveContainer = masterContainer.copy(
      env = Option(distinctOnName(runInfoEnvVar +: masterContainer.env.toSeq.flatten)),
      workingDir = Option(OrchestraConfig.workspace),
      volumeMounts = Option(distinctOnName(masterContainer.volumeMounts.toSeq.flatten :+ JobSpecUtils.homeDirMount))
    )

    JobSpec(
      template = PodTemplateSpec(
        spec = Option(
          PodSpec(
            nodeSelector = Option(podConfig.nodeSelector),
            containers = slaveContainer +: podConfig.containerSeq.map(createContainer(_, masterContainer)),
            volumes = Option(
              distinctOnName(podConfig.volumes ++ masterSpec.volumes.toSeq.flatten :+ JobSpecUtils.homeDirVolume)
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
