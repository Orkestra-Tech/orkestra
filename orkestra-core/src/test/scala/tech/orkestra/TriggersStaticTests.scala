package tech.orkestra

import shapeless._
import shapeless.test.illTyped
import tech.orkestra.Dsl._
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.Triggers._

object TriggersStaticTests {

  object `Trigger an empty job` {
    emptyJob.trigger(HNil)
    emptyJob.run(HNil)
  }

  object `Trigger a job with one parameter` {
    oneParamJob.trigger("someString" :: HNil)
    oneParamJob.run("someString" :: HNil)
  }

  object `Trigger a job with multiple parameters` {
    twoParamsJob.trigger("some string" :: true :: HNil)
    twoParamsJob.run("some string" :: true :: HNil)
  }

  object `Trigger a job with 1 parameter not given should not compile` {
    illTyped("""
      twoParamsJob.trigger("some string" :: HNil)
    """, "type mismatch;.+")
    illTyped("""
      twoParamsJob.run("some string" :: HNil)
    """, "type mismatch;.+")
  }

  object `Trigger a job with 1 parameter not of the same type should not compile` {
    illTyped("""
      twoParamsJob.trigger("some string" :: "I should be of type boolean" :: HNil)
    """, "type mismatch;.+")
    illTyped("""
      twoParamsJob.run("some string" :: "I should be of type boolean" :: HNil)
    """, "type mismatch;.+")
  }
}
