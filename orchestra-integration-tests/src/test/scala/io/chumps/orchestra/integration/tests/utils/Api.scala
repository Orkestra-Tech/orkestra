package io.chumps.orchestra.integration.tests.utils

import shapeless.HList

import io.chumps.orchestra.{CommonApi, Jobs}
import io.chumps.orchestra.board.Job

object Api {
  def jobClient[ParamValues <: HList, Result](job: Job[ParamValues, Result, _, _]) =
    AutowireClient(Kubernetes.client, s"${Jobs.jobSegment}/${job.id.value}")[job.Api]

  val commonClient = AutowireClient(Kubernetes.client, Jobs.commonSegment)[CommonApi]
}
