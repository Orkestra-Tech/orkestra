//package tech.orkestra.integration.tests
//
//import java.time.Instant
//
//import autowire._
//import io.circe.generic.auto._
//import io.circe.java8.time._
//import io.circe.shapes._
//import org.scalatest._
//import org.scalatest.Matchers._
//import org.scalatest.OptionValues._
//import shapeless._
//
//import tech.orkestra.integration.tests.utils._
//import tech.orkestra.model.{Page, RunId}
//import tech.orkestra.utils.AkkaImplicits._
//
//class AllTests extends FeatureSpec with IntegrationTest {
//
//  scenario("Empty history") {
//    Api.jobClient(SomeJob.board).history(Page[Instant](None, -50)).call().futureValue.runs shouldBe empty
//  }
//
//  scenario("Run a job") {
//    Api.jobClient(SomeJob.board).trigger(RunId.random(), HNil: HNil).call().futureValue
//
//    // Check triggered state
//    eventually {
//      val response = Api.jobClient(SomeJob.board).history(Page[Instant](None, -50)).call().futureValue
//      (response.runs should have).size(1)
//      val run = response.runs.headOption.value._1
//      run.triggeredOn should ===(run.latestUpdateOn)
//      run.result should ===(None)
//    }
//
//    // Check running state
//    eventually {
//      val response = Api.jobClient(SomeJob.board).history(Page[Instant](None, -50)).call().futureValue
//      (response.runs should have).size(1)
//      val run = response.runs.headOption.value._1
//      run.triggeredOn should not be run.latestUpdateOn
//      run.result should ===(None)
//    }
//
//    // Check success state
//    eventually {
//      val response = Api.jobClient(SomeJob.board).history(Page[Instant](None, -50)).call().futureValue
//      (response.runs should have).size(1)
//      response.runs.headOption.value._1.result should ===(Some(Right(())))
//    }
//  }
//}
