package com.drivetribe.orchestra

import shapeless.test.illTyped

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board.Job
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.{JobId, RunId}
import com.drivetribe.orchestra.parameter.{Checkbox, Input}

object ParametersStaticTests {

  object `Define a job only with RunId` {
    lazy val job = Job[RunId => Unit](JobId("someJob"), "Some Job")()

    lazy val jobRunner = JobRunner(job) { implicit workDir => runId =>
      println(s"RunId: $runId")
    }
  }

  object `Define a job with one parameter and RunId` {
    lazy val job = Job[(String, RunId) => Unit](JobId("someJob"), "Some Job")(Input[String]("Some string"))

    lazy val jobRunner = JobRunner(job) { implicit workDir => (s, runId) =>
      println(s"Some string: $s")
      println(s"RunId: $runId")
    }
  }

  object `Define a job with RunId and one parameter` {
    lazy val job = Job[(RunId, String) => Unit](JobId("someJob"), "Some Job")(Input[String]("Some string"))

    lazy val jobRunner = JobRunner(job) { implicit workDir => (runId, s) =>
      println(s"RunId: $runId")
      println(s"Some string: $s")
    }
  }

  object `Define a job with multiple parameters and RunId` {
    lazy val job =
      Job[(Boolean, String, RunId) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                          Input[String]("Some string"))

    lazy val jobRunner = JobRunner(job) { implicit workDir => (someBoolean, someString, runId) =>
      println(s"Some boolean: $someBoolean")
      println(s"Some string: $someString")
      println(s"RunId: $runId")
    }
  }

  object `Should not compile if 1 UI parameter is not given` {
    illTyped("""
      lazy val job = Job[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"))
    """)
  }

  object `Should not compile if 1 parameter value is not given` {
    lazy val job =
      Job[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                   Input[String]("Some string"))

    illTyped("""
      lazy val jobRunner = JobRunner(job) { implicit workDir => someBoolean =>
        println(s"Some boolean: $someBoolean")
      }
    """)
  }

  object `Should not compile if 1 UI parameter is not of the same type` {
    illTyped("""
      lazy val job =
        Job[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                     Checkbox("Some string"))
    """)
  }

  object `Should not compile if 1 parameter value is not of the same type` {
    lazy val job =
      Job[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                   Input[String]("Some string"))

    illTyped("""
      lazy val jobRunner = JobRunner(job) { implicit workDir => (someBoolean: Boolean, someWrongType: Boolean) =>
        println(s"Some boolean: $someBoolean")
      }
    """)
  }
}
