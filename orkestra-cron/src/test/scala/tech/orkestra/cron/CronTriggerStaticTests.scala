package tech.orkestra.cron

import cats.effect.{ContextShift, IO}
import shapeless._
import shapeless.test.illTyped
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.OrkestraConfigTest

import scala.concurrent.ExecutionContext

object CronTriggerStaticTests extends OrkestraConfigTest {
  implicit lazy val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  object `Define a CronTrigger with job that has no parameters` {
    CronTrigger("*/5 * * * *", emptyJob, HNil)
  }

  object `Define a CronTrigger with one parameter` {
    CronTrigger("*/5 * * * *", oneParamJob, "some string" :: HNil)
  }

  object `Define a CronTrigger with multiple parameters` {
    CronTrigger("*/5 * * * *", twoParamsJob, "some string" :: true :: HNil)
  }

  object `Define a CronTrigger with 1 parameter not given should not compile` {
    illTyped(
      """
      CronTrigger("*/5 * * * *", oneParamJob, HNil)
      """,
      "type mismatch;.+"
    )
  }

  object `Define a CronTrigger with 1 parameter not of the same type should not compile` {
    illTyped(
      """
      CronTrigger("*/5 * * * *", twoParamsJob, "some string" :: "I should be of type boolean" :: HNil)
      """,
      "type mismatch;.+"
    )
  }
}
