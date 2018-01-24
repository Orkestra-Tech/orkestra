package io.chumps.orchestra.model

import com.sksamuel.elastic4s.http.ElasticDsl.keywordField
import io.circe.parser._
import io.circe.generic.auto._
import io.k8s.api.batch.v1.{Job => KubeJob}

case class RunInfo(jobId: JobId, runId: RunId)

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

  lazy val elasticsearchFields = Seq(
    keywordField("jobId"),
    keywordField("runId"),
  )
}

case class EnvRunInfo(jobId: JobId, runId: Option[RunId])
