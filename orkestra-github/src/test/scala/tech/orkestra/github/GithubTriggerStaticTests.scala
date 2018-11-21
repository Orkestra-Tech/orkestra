package tech.orkestra.github

import shapeless._
import shapeless.test.illTyped
import tech.orkestra.utils.DummyJobs._
import tech.orkestra.utils.OrkestraConfigTest

object GithubTriggerStaticTests extends OrkestraConfigTest {

  object `Define a GithubTrigger with 1 parameter without default should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", oneParamJob, HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), oneParamJob, HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
  }

  object `Define a GithubTrigger with 1 default not of the same type should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", twoParamsJob, true :: true :: HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), twoParamsJob, "someString" :: "someWrong" :: HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
  }

  object `Define a GithubTrigger with too many defaults should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", twoParamsJob, "someString" :: true :: true :: HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), twoParamsJob, "someString" :: true :: "someTooMuch" :: HNil)
      """,
      "could not find implicit value for parameter gitRefInjector:.+"
    )
  }
}
