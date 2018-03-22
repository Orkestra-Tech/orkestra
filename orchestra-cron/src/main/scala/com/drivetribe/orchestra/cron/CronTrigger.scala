package com.drivetribe.orchestra.cron

import shapeless.HNil

import com.drivetribe.orchestra.job.JobRunner

case class CronTrigger(schedule: String, jobRunner: JobRunner[HNil, _])
