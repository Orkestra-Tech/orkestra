package io.chumps.orchestra.cron

import io.chumps.orchestra.Job
import shapeless.{HList, HNil}

case class CronTrigger(schedule: String, job: Job.Runner[HNil, _, _ <: HList])
