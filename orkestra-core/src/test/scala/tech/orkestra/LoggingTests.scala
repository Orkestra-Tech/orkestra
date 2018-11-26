//package tech.orkestra
//
//import java.io.PrintStream
//
//import cats.effect.IO
//
//import scala.concurrent.Future
//import io.k8s.api.core.v1.Container
//import org.scalatest.Matchers._
//import org.scalatest.OptionValues._
//import shapeless.HNil
//import tech.orkestra.filesystem.Implicits.workDir
//import tech.orkestra.job.Jobs
//import tech.orkestra.model.{Page, RunId}
//import tech.orkestra.utils._
//import tech.orkestra.utils.AkkaImplicits._
//import org.scalatest.concurrent.Eventually
//
//class LoggingTests
//    extends OrkestraSpec
//    with OrkestraConfigTest
//    with KubernetesTest
//    with ElasticsearchTest
//    with Shells[IO]
//    with Stages
//    with Eventually {
//
//  scenario("Log stuff and get it back") {
//    val message = "Log stuff and get it back"
//    Jobs.withOutErr(
//      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orkestraConfig.runInfo.runId), true)
//    ) {
//      println(message)
//      println(
//        "A\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA\nA"
//      )
//    }
//
//    eventually {
//      val logs = CommonApiServer().logs(orkestraConfig.runInfo.runId, Page(None, 10000)).futureValue
//      (logs should have).size(104)
//      logs.headOption.value.line should ===(message)
//    }
//  }
//
//  scenario("No logs for run that logged nothing") {
//    val logs = CommonApiServer().logs(RunId.random(), Page(None, 10000)).futureValue
//    logs shouldBe empty
//  }
//
//  scenario("Log stuff in a stage and get it back") {
//    DummyJobs.emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
//
//    val stageName = "Log stuff in a stage and get it back"
//    val message = "Log stuff in a stage and get it back"
//    Jobs.withOutErr(
//      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orkestraConfig.runInfo.runId), true)
//    ) {
//      stage(stageName) {
//        println(message)
//      }
//    }
//
//    eventually {
//      val logs = CommonApiServer().logs(orkestraConfig.runInfo.runId, Page(None, 10000)).futureValue
//      (logs should have).size(2)
//
//      logs.headOption.value.line should ===(s"Stage: $stageName")
//      logs(1).line should ===(message)
//    }
//  }
//
//  scenario("Log stuff in parallel stages and get it back") {
//    DummyJobs.emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
//
//    val stage1Name = "Log stuff in parallel stages and get it back 1"
//    val stage2Name = "Log stuff in parallel stages and get it back 2"
//    val message1 = "Log stuff in parallel stages and get it back 1"
//    val message2 = "Log stuff in parallel stages and get it back 2"
//    Jobs.withOutErr(
//      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orkestraConfig.runInfo.runId), true)
//    ) {
//      Future
//        .sequence(
//          Seq(
//            Future(stage(stage1Name) {
//              println(message1)
//            }),
//            Future(stage(stage2Name) {
//              println(message2)
//            })
//          )
//        )
//        .futureValue
//    }
//
//    eventually {
//      val logs = CommonApiServer().logs(orkestraConfig.runInfo.runId, Page(None, 10000)).futureValue
//      (logs should have).size(4)
//
//      logs.map(_.line) should contain(s"Stage: $stage1Name")
//      logs.map(_.line) should contain(message1)
//      logs.map(_.line) should contain(s"Stage: $stage2Name")
//      logs.map(_.line) should contain(message2)
//    }
//  }
//
//  scenario("Log stuff with a shell command in a container and in a stage and it back") {
//    DummyJobs.emptyJob.ApiServer().trigger(orkestraConfig.runInfo.runId, HNil).futureValue
//
//    val stageName = "Log stuff with a shell command in a container and in a stage and it back"
//    Jobs.withOutErr(
//      new PrintStream(new ElasticsearchOutputStream(elasticsearchClient, orkestraConfig.runInfo.runId), true)
//    ) {
//      stage(stageName) {
//        sh("echo Hello", Container("someContainer")).unsafeRunSync()
//      }
//    }
//
//    eventually {
//      val logs = CommonApiServer().logs(orkestraConfig.runInfo.runId, Page(None, 10000)).futureValue
//      logs.headOption.value.line should ===(s"Stage: $stageName")
//      logs(1).line should ===("Running: echo Hello")
//    }
//  }
//}
