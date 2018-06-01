package com.goyeau.orkestra.cron

import shapeless.HNil

import com.goyeau.orkestra.job.Job

/**
  * A cron triggerable job.
  *
  * @param schedule The cron schedule expression
  * @param job The job to trigger
  */
case class CronTrigger(schedule: String, job: Job[HNil, _])
