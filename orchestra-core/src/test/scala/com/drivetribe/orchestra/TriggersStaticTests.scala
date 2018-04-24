package com.drivetribe.orchestra

import shapeless.test.illTyped

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.utils.DummyJobs._
import com.drivetribe.orchestra.utils.Triggers._

object TriggersStaticTests {

  object `Trigger a job only with RunId` {
    emptyWithRunIdJob.trigger()
    emptyWithRunIdJob.run()
  }

  object `Define job with one parameter and RunId` {
    oneParamWithRunIdJob.trigger("someString")
    oneParamWithRunIdJob.run("someString")
  }

  object `Define job with RunId and one parameter` {
    runIdWithOneParamJob.trigger("someString")
    runIdWithOneParamJob.run("someString")
  }

  object `Define job with multiple parameters and RunId` {
    twoParamsWithRunIdJob.trigger("someString", true)
    twoParamsWithRunIdJob.run("someString", true)
  }

  object `Should not compile if 1 parameter is not given` {
    illTyped("""
      twoParamsWithRunIdJob.trigger("someString")
    """, "type mismatch;.+")
    illTyped("""
      twoParamsWithRunIdJob.run("someString")
    """, "type mismatch;.+")
  }

  object `Should not compile if 1 parameter is not of the same type` {
    illTyped("""
      twoParamsWithRunIdJob.trigger("someString", "true")
    """, """too many arguments \(2\) for method trigger:.+""")
    illTyped("""
      twoParamsWithRunIdJob.run("someString", "true")
    """, """too many arguments \(2\) for method run:.+""")
  }
}
