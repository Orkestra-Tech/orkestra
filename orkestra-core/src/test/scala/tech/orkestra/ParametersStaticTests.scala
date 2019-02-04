package tech.orkestra

import shapeless.test.illTyped
import tech.orkestra.job.Job
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.OrkestraConfigTest

object ParametersStaticTests extends OrkestraConfigTest {

  object `Define a job with 1 parameter value not given should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) { case someBoolean :: HNil =>
        ()
      }
      """,
      "constructor cannot be instantiated to expected type;.+"
    )
  }

  object `Define a job with 1 parameter value not of the same type should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) {
        case (someString: String) :: (someWrongType: Boolean) :: HNil =>
          ()
      }
      """,
      "constructor cannot be instantiated to expected type;.+"
    )
  }

  object `Define a job with too many parameters value should not compile` {
    illTyped(
      """
      lazy val job = Job(twoParamsJobBoard) {
        case someString :: someBoolean :: someOther :: HNil =>
          ()
      }
      """,
      "constructor cannot be instantiated to expected type;.+"
    )
  }
}
