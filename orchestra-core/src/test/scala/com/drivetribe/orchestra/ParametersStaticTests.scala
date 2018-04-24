package com.drivetribe.orchestra

import shapeless.test.illTyped

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.board.JobBoard
import com.drivetribe.orchestra.job.Job
import com.drivetribe.orchestra.model.{JobId, RunId}
import com.drivetribe.orchestra.parameter.{Checkbox, Input}

object ParametersStaticTests {

  object `Define a job only with RunId` {
    lazy val board = JobBoard[RunId => Unit](JobId("someJob"), "Some Job")()

    lazy val job = Job(board) { implicit workDir => runId =>
      println(s"RunId: $runId")
    }
  }

  object `Define a job with one parameter and RunId` {
    lazy val board = JobBoard[(String, RunId) => Unit](JobId("someJob"), "Some Job")(Input[String]("Some string"))

    lazy val job = Job(board) { implicit workDir => (s, runId) =>
      println(s"Some string: $s")
      println(s"RunId: $runId")
    }
  }

  object `Define a job with RunId and one parameter` {
    lazy val board = JobBoard[(RunId, String) => Unit](JobId("someJob"), "Some Job")(Input[String]("Some string"))

    lazy val job = Job(board) { implicit workDir => (runId, s) =>
      println(s"RunId: $runId")
      println(s"Some string: $s")
    }
  }

  object `Define a job with multiple parameters and RunId` {
    lazy val board =
      JobBoard[(Boolean, String, RunId) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                               Input[String]("Some string"))

    lazy val job = Job(board) { implicit workDir => (someBoolean, someString, runId) =>
      println(s"Some boolean: $someBoolean")
      println(s"Some string: $someString")
      println(s"RunId: $runId")
    }
  }

  object `Should not compile if 1 UI parameter is not given` {
    illTyped(
      """
      lazy val board = JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"))
      """,
      "could not find implicit value for parameter paramOperations:.+"
    )
  }

  object `Should not compile if 1 parameter value is not given` {
    lazy val board =
      JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                        Input[String]("Some string"))

    illTyped(
      """
      lazy val job = Job(board) { implicit workDir => someBoolean =>
        println(s"Some boolean: $someBoolean")
      }
      """,
      "missing parameter type"
    )
  }

  object `Should not compile if 1 UI parameter is not of the same type` {
    illTyped(
      """
      lazy val board =
        JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                          Checkbox("Some string"))
      """,
      "could not find implicit value for parameter paramOperations:.+"
    )
  }

  object `Should not compile if 1 parameter value is not of the same type` {
    lazy val board =
      JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"),
                                                                        Input[String]("Some string"))

    illTyped(
      """
      lazy val job = Job(board) { implicit workDir => (someBoolean: Boolean, someWrongType: Boolean) =>
        println(s"Some boolean: $someBoolean")
      }
      """,
      "type mismatch;.+"
    )
  }
}
