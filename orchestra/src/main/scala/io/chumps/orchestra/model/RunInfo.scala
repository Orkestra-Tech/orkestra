package io.chumps.orchestra.model

import io.circe.{Decoder, Encoder}
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import io.k8s.api.batch.v1.{Job => KubeJob}

case class RunInfo(jobId: Symbol, runId: RunId)

object RunInfo {

  def fromKubeJob(job: KubeJob): RunInfo =
    (
      for {
        jobSpec <- job.spec
        podSpec <- jobSpec.template.spec
        container <- podSpec.containers.headOption
        envs <- container.env
        env <- envs.find(_.name == "ORCHESTRA_RUN_INFO")
        runInfoJson <- env.value
        jobUid = RunId(job.metadata.get.uid.get)
      } yield
        decode[EnvRunInfo](runInfoJson)
          .fold(throw _, runInfo => RunInfo(runInfo.jobId, runInfo.runId.getOrElse(jobUid)))
    ).getOrElse(throw new IllegalArgumentException(s"Wrong job format: $job"))

  implicit val encoder: Encoder[RunInfo] = deriveEncoder
  implicit val decoder: Decoder[RunInfo] = deriveDecoder
}

case class EnvRunInfo(jobId: Symbol, runId: Option[RunId])
