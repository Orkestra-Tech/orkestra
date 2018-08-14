package tech.orkestra.cron

import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.OrkestraConfigTest
import shapeless.test.illTyped

object CronTriggerStaticTests extends OrkestraConfigTest {

  object `Define a CronTrigger with job that has no parameters` {
    CronTrigger("*/5 * * * *", emptyJob)()
  }

  object `Define a CronTrigger with one parameter` {
    CronTrigger("*/5 * * * *", oneParamJob)("some string")
  }

  object `Define a CronTrigger with multiple parameters` {
    CronTrigger("*/5 * * * *", twoParamsJob)("some string", true)
  }

  object `Define a CronTrigger with 1 parameter not given should not compile` {
    illTyped(
      """
      CronTrigger("*/5 * * * *", oneParamJob)()
      """,
      "could not find implicit value for parameter defaultParamsWitness:.+"
    )
  }

  object `Define a CronTrigger with 1 parameter not of the same type should not compile` {
    illTyped(
      """
      CronTrigger("*/5 * * * *", twoParamsJob)("some string", "I should be of type boolean")
      """,
      "could not find implicit value for parameter tupleToHList:.+"
    )
  }
}
