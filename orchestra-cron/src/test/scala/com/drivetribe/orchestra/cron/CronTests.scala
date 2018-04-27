package com.drivetribe.orchestra.cron

import com.drivetribe.orchestra.utils.DummyJobs._
import com.drivetribe.orchestra.utils._
import org.scalatest.Matchers._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.Eventually

class CronTests extends OrchestraSpec with OrchestraConfigTest with KubernetesTest with Eventually {

  scenario("Schedule a Cron") {
    CronJobs.createOrUpdate(Set(CronTrigger("*/5 * * * *", emptyJob))).futureValue
    val scheduledCronJobs = CronJobs.list().futureValue.items
    scheduledCronJobs should have size 1
  }

  scenario("No cron job scheduled") {
    val scheduledCronJobs = CronJobs.list().futureValue.items
    scheduledCronJobs should have size 0
  }

  scenario("Remove a cron job") {
    val someCronJobs = Set(
      CronTrigger("*/5 * * * *", emptyJob),
      CronTrigger("*/10 * * * *", emptyJob2)
    )

    CronJobs.createOrUpdate(someCronJobs).futureValue
    CronJobs.list().futureValue.items should have size someCronJobs.size

    CronJobs.deleteStale(someCronJobs.drop(1)).futureValue
    val cronJobs = CronJobs.list().futureValue.items
    cronJobs should have size someCronJobs.size - 1
    cronJobs.head.spec.value.schedule should ===(someCronJobs.last.schedule)
  }
}
