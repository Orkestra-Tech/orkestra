package com.drivetribe.orchestra

import shapeless.test.illTyped

import com.drivetribe.orchestra.Dsl._
import com.drivetribe.orchestra.utils.DummyJobs._

object TriggersStaticTests {

  object `Trigger a job only with RunId` {
    emptyWithRunIdJobRunner.trigger()
    emptyWithRunIdJobRunner.run()
  }

  object `Define job with one parameter and RunId` {
    oneParamWithRunIdJobRunner.trigger("someString")
    oneParamWithRunIdJobRunner.run("someString")
  }

  object `Define job with RunId and one parameter` {
    runIdWithOneParamJobRunner.trigger("someString")
    runIdWithOneParamJobRunner.run("someString")
  }

  object `Define job with multiple parameters and RunId` {
    twoParamsWithRunIdJobRunner.trigger("someString", true)
    twoParamsWithRunIdJobRunner.run("someString", true)
  }

  object `Should not compile if 1 parameter is not given` {
    illTyped("""
      twoParamsWithRunIdJobRunner.trigger("someString")
    """)
    illTyped("""
      twoParamsWithRunIdJobRunner.run("someString")
    """)
  }

  object `Should not compile if 1 parameter is not of the same type` {
    illTyped("""
      twoParamsWithRunIdJobRunner.trigger("someString", "true")
    """)
    illTyped("""
      twoParamsWithRunIdJobRunner.run("someString", "true")
    """)
  }
}
