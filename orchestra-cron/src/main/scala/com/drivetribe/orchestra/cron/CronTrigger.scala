package com.drivetribe.orchestra.cron

import shapeless.HNil

import com.drivetribe.orchestra.job.JobRunner

/**
  * A cron triggerable job.
  *
  * @param schedule The cron schedule expression
  * @param jobRunner The job to trigger
  */
case class CronTrigger(schedule: String, jobRunner: JobRunner[HNil, _])
