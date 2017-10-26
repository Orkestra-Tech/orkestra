package io.chumps.orchestra.cron

import shapeless.HNil

import io.chumps.orchestra.job.JobRunner

case class CronTrigger(schedule: String, jobRunner: JobRunner[HNil, _])
