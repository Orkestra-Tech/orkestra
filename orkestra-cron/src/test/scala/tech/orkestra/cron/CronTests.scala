//package tech.orkestra.cron
//
//import org.scalatest.Matchers._
//import org.scalatest.OptionValues._
//import org.scalatest.concurrent.Eventually
//import shapeless._
//import tech.orkestra.utils._
//import tech.orkestra.utils.DummyJobs._
//
//class CronTests extends OrkestraSpec with OrkestraConfigTest with KubernetesTest with Eventually {
//
//  scenario("Schedule a cron job") {
//    val someCronJob = CronTrigger("*/5 * * * *", emptyJob, HNil)
//
//    CronJobs.createOrUpdate(Set(someCronJob)).futureValue
//    val cronJobs = CronJobs.list().futureValue.items
//    (cronJobs should have).size(1)
//    cronJobs.head.spec.value.schedule should ===(someCronJob.schedule)
//  }
//
//  scenario("Update a cron job") {
//    val someCronJob = CronTrigger("*/5 * * * *", emptyJob, HNil)
//
//    CronJobs.createOrUpdate(Set(someCronJob)).futureValue
//    val cronJobs = CronJobs.list().futureValue.items
//    (cronJobs should have).size(1)
//    cronJobs.head.spec.value.schedule should ===(someCronJob.schedule)
//
//    // Update
//    val newCronJob = CronTrigger("*/10 * * * *", emptyJob, HNil)
//    CronJobs.createOrUpdate(Set(newCronJob)).futureValue
//    val updatedCronJobs = CronJobs.list().futureValue.items
//    (updatedCronJobs should have).size(1)
//    updatedCronJobs.head.spec.value.schedule should ===(newCronJob.schedule)
//  }
//
//  scenario("No cron job scheduled") {
//    val scheduledCronJobs = CronJobs.list().futureValue.items
//    (scheduledCronJobs should have).size(0)
//  }
//
//  scenario("Remove a cron job") {
//    val someCronJobs = Set[CronTrigger](
//      CronTrigger("*/5 * * * *", emptyJob, HNil),
//      CronTrigger("*/10 * * * *", emptyJob2, HNil)
//    )
//
//    CronJobs.createOrUpdate(someCronJobs).futureValue
//    (CronJobs.list().futureValue.items should have).size(someCronJobs.size)
//
//    CronJobs.deleteStale(someCronJobs.drop(1)).futureValue
//    val cronJobs = CronJobs.list().futureValue.items
//    (cronJobs should have).size(someCronJobs.size - 1)
//    cronJobs.head.spec.value.schedule should ===(someCronJobs.last.schedule)
//  }
//}
