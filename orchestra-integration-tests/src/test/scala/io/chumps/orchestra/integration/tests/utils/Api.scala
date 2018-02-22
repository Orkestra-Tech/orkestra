package io.chumps.orchestra.integration.tests.utils

import shapeless.HList

import io.chumps.orchestra.{CommonApi, OrchestraConfig}
import io.chumps.orchestra.board.Job

object Api {
  def jobClient[ParamValues <: HList, Result](job: Job[ParamValues, Result, _, _]) =
    AutowireClient(Kubernetes.client, s"${OrchestraConfig.jobSegment}/${job.id.value}")[job.Api]

  val commonClient = AutowireClient(Kubernetes.client, OrchestraConfig.commonSegment)[CommonApi]
}
