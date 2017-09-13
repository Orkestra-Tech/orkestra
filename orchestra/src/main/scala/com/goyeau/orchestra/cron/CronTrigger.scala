package com.goyeau.orchestra.cron

import com.goyeau.orchestra.Job
import shapeless.{HList, HNil}

case class CronTrigger(schedule: String, job: Job.Runner[HNil, _, _ <: HList])
