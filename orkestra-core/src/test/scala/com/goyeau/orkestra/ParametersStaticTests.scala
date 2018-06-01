package com.goyeau.orkestra

import shapeless.test.illTyped
import com.goyeau.orkestra.board.JobBoard
import com.goyeau.orkestra.job.Job
import com.goyeau.orkestra.model.JobId
import com.goyeau.orkestra.parameter.{Checkbox, Input}
import com.goyeau.orkestra.utils.DummyJobs._
import com.goyeau.orkestra.utils.OrkestraConfigTest

object ParametersStaticTests extends OrkestraConfigTest {

  object `Define a job with 1 UI parameter not given should not compile` {
    illTyped(
      """
      lazy val board = JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some string"))
      """,
      "could not find implicit value for parameter paramOperations:.+"
    )
  }

  object `Define a job with 1 parameter value not given should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) { implicit workDir => someBoolean =>
        ()
      }
      """,
      "missing parameter type"
    )
  }

  object `Define a job with 1 UI parameter not of the same type should not compile` {
    illTyped(
      """
      lazy val board = JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Checkbox("Some Boolean"),
                                                                                         Checkbox("Some Boolean 2"))
      """,
      "could not find implicit value for parameter paramOperations:.+"
    )
  }

  object `Define a job with 1 parameter value not of the same type should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) { implicit workDir => (someBoolean: Boolean, someWrongType: Boolean) =>
        ()
      }
      """,
      "type mismatch;.+"
    )
  }

  object `Define a job with too many UI parameters should not compile` {
    illTyped(
      """
      lazy val board = JobBoard[(Boolean, String) => Unit](JobId("someJob"), "Some Job")(Input("Some String"),
                                                                                         Checkbox("Some Boolean"),
                                                                                         Checkbox("Some other"))
      """,
      """too many arguments \(3\) for method apply:.+"""
    )
  }

  object `Define a job with too many parameters value should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) {
        implicit workDir => (someString: String, someBoolean: Boolean, someOther: String) =>
          ()
      }
      """,
      "missing parameter type"
    )
  }
}
