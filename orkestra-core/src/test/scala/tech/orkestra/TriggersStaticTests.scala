package tech.orkestra

import shapeless.test.illTyped

import tech.orkestra.Dsl._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.Triggers._

object TriggersStaticTests {

  object `Trigger an empty job` {
    emptyJob.trigger()
    emptyJob.run()
  }

  object `Trigger a job with one parameter` {
    oneParamJob.trigger("someString")
    oneParamJob.run("someString")
  }

  object `Trigger a job with multiple parameters` {
    twoParamsJob.trigger("some string", true)
    twoParamsJob.run("some string", true)
  }

  object `Trigger a job with 1 parameter not given should not compile` {
    illTyped("""
      twoParamsJob.trigger("some string")
    """, "type mismatch;.+")
    illTyped("""
      twoParamsJob.run("some string")
    """, "type mismatch;.+")
  }

  object `Trigger a job with 1 parameter not of the same type should not compile` {
    illTyped("""
      twoParamsJob.trigger("some string", "I should be of type boolean")
    """, """too many arguments \(2\) for method trigger:.+""")
    illTyped("""
      twoParamsJob.run("some string", "I should be of type boolean")
    """, """too many arguments \(2\) for method run:.+""")
  }
}
