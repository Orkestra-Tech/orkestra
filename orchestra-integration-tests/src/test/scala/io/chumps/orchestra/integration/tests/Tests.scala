package io.chumps.orchestra.integration.tests

import java.time.Instant

import scala.concurrent.duration._

import autowire._
import io.circe.generic.auto._
import io.circe.java8.time._
import io.circe.shapes._
import org.scalatest._
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures

import io.chumps.orchestra.integration.tests.utils._
import io.chumps.orchestra.model.Page
import io.chumps.orchestra.utils.AkkaImplicits._

class Tests extends FeatureSpec with BeforeAndAfter with ScalaFutures {

  before {
    (for {
      _ <- DeployElasticsearch(Kubernetes.client)
      _ <- DeployOrchestration(Kubernetes.client)
    } yield ()).futureValue(timeout(5.minutes))
  }

  after {
    Kubernetes.client.namespaces.delete(Kubernetes.namespace.metadata.get.name.get).futureValue
  }

  scenario("Integration Test") {
    Api
      .jobClient(SomeJob.job)
      .history(Page[Instant](None, -50))
      .call()
      .futureValue(timeout(1.minute))
      .runs shouldBe empty
  }
}
