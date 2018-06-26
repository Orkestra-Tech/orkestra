package tech.orkestra

import shapeless.test.illTyped

import tech.orkestra.Dsl._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.Triggers._

object TriggersStaticTests {

  object `Trigger a job only with RunId` {
    emptyWithRunIdJob.trigger()
    emptyWithRunIdJob.run()
  }

  object `Trigger a job with one parameter and RunId` {
    oneParamWithRunIdJob.trigger("someString")
    oneParamWithRunIdJob.run("someString")
  }

  object `Trigger a job with RunId and one parameter` {
    runIdWithOneParamJob.trigger("someString")
    runIdWithOneParamJob.run("someString")
  }

  object `Trigger a job with multiple parameters and RunId` {
    twoParamsWithRunIdJob.trigger("someString", true)
    twoParamsWithRunIdJob.run("someString", true)
  }

  object `Trigger a job with 1 parameter not given should not compile` {
    illTyped("""
      twoParamsWithRunIdJob.trigger("someString")
    """, "type mismatch;.+")
    illTyped("""
      twoParamsWithRunIdJob.run("someString")
    """, "type mismatch;.+")
  }

  object `Trigger a job with 1 parameter not of the same type should not compile` {
    illTyped("""
      twoParamsWithRunIdJob.trigger("someString", "true")
    """, """too many arguments \(2\) for method trigger:.+""")
    illTyped("""
      twoParamsWithRunIdJob.run("someString", "true")
    """, """too many arguments \(2\) for method run:.+""")
  }
}
