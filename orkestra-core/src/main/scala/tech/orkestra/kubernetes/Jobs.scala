package tech.orkestra.kubernetes

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import com.goyeau.kubernetes.client.KubernetesClient
import cats.implicits._
import io.k8s.api.batch.v1.{Job => KubeJob}
import io.k8s.api.core.v1.PodSpec
import io.k8s.apimachinery.pkg.apis.meta.v1.{DeleteOptions, ObjectMeta}
import org.http4s.Status._
import tech.orkestra.OrkestraConfig
import tech.orkestra.model.{EnvRunInfo, RunInfo}

private[orkestra] object Jobs {

  def name(runInfo: RunInfo) =
    s"${runInfo.jobId.value.toLowerCase}-${runInfo.runId.value.toString.split("-").head}"

  def create[F[_]: Sync](
    runInfo: RunInfo,
    podSpec: PodSpec
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[Unit] =
    for {
      masterPod <- MasterPod.get
      job = KubeJob(
        metadata = Option(ObjectMeta(name = Option(name(runInfo)))),
        spec = Option(JobSpecs.create(masterPod, EnvRunInfo(runInfo.jobId, Option(runInfo.runId)), podSpec))
      )
      _ <- kubernetesClient.jobs.namespace(orkestraConfig.namespace).create(job)
    } yield ()

  def delete[F[_]: Sync](
    runInfo: RunInfo
  )(implicit orkestraConfig: OrkestraConfig, kubernetesClient: KubernetesClient[F]): F[Unit] = {
    val jobs = kubernetesClient.jobs.namespace(orkestraConfig.namespace)

    for {
      jobList <- jobs.list
      job = jobList.items.find(RunInfo.fromKubeJob(_) == runInfo)

      _ <- job.fold(Applicative[F].pure(Ok)) { job =>
        jobs.delete(
          job.metadata.get.name.get,
          Option(DeleteOptions(propagationPolicy = Option("Foreground"), gracePeriodSeconds = Option(0)))
        )
      }
    } yield ()
  }
}
