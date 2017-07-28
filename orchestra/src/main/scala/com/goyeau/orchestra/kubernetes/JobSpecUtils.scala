package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.{AutowireServer, Config, Container, PodConfig, RunInfo}
import io.fabric8.kubernetes.api.model._
import scala.collection.convert.ImplicitConversions._
import scala.language.reflectiveCalls

import io.fabric8.kubernetes.api.model.{Container => KubeContainer}
import io.circe.generic.auto._

object JobSpecUtils {
  private val homeDirMount = {
    val volumeMount = new VolumeMount()
    volumeMount.setName("home")
    volumeMount.setMountPath(Config.workspace)
    volumeMount
  }
  private val homeDirVolume = {
    val volume = new Volume()
    volume.setName("home")
    volume.setEmptyDir(new EmptyDirVolumeSource())
    volume
  }

  private def createContainer(container: Container, masterContainer: KubeContainer): KubeContainer = {
    val kubeContainer = new KubeContainer()
    kubeContainer.setName(container.name)
    kubeContainer.setImage(container.image)
    kubeContainer.setEnv(masterContainer.getEnv)
    kubeContainer.setStdin(true)
    kubeContainer.setTty(true)
    kubeContainer.setCommand(container.command)
    kubeContainer.setWorkingDir(Config.workspace)
    kubeContainer.setVolumeMounts(masterContainer.getVolumeMounts)
    kubeContainer
  }

  def createJobSpec(masterPod: Pod, runInfo: RunInfo, podConfig: PodConfig[_]) = {
    val masterSpec = masterPod.getSpec
    val masterContainer = masterSpec.getContainers.head
    val runInfoEnvVar = new EnvVar("ORCHESTRA_RUN_INFO", AutowireServer.write(runInfo), null)
    masterContainer.setEnv(distinctOnName(runInfoEnvVar +: masterContainer.getEnv))
    masterContainer.setWorkingDir(Config.workspace)
    masterContainer.setVolumeMounts(distinctOnName(masterContainer.getVolumeMounts :+ JobSpecUtils.homeDirMount))

    val jobSpec = new JobSpec()
    jobSpec.setTemplate {
      val podTemplateSpec = new PodTemplateSpec()
      podTemplateSpec.setSpec {
        val podSpec = new PodSpec()
        podSpec.setContainers(masterContainer +: podConfig.containerSeq.map(createContainer(_, masterContainer)))
        podSpec.setVolumes(distinctOnName(masterSpec.getVolumes :+ JobSpecUtils.homeDirVolume))
        podSpec.setRestartPolicy("Never")
        podSpec
      }
      podTemplateSpec
    }
    jobSpec
  }

  private def distinctOnName[A <: { def getName(): String }](list: Seq[A]): Seq[A] =
    list.groupBy(_.getName()).values.map(_.head).toSeq
}
