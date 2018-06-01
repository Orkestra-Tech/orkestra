package com.goyeau.orkestra.github

import com.goyeau.orkestra.utils.DummyJobs._
import com.goyeau.orkestra.utils.OrkestraConfigTest
import shapeless.test.illTyped

object GithubTriggerStaticTests extends OrkestraConfigTest {

  object `Define a GithubTrigger with 1 parameter without default should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", oneParamJob)()
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), oneParamJob)()
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
  }

  object `Define a GithubTrigger with 1 default not of the same type should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", twoParamsJob)(true, true)
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), twoParamsJob)("someString", "someWrong")
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
  }

  object `Define a GithubTrigger with too many defaults should not compile` {
    illTyped(
      """
      BranchTrigger(Repository("someRepo"), "some-branch", twoParamsJob)("someString", true, true)
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
    illTyped(
      """
      PullRequestTrigger(Repository("someRepo"), twoParamsJob)("someString", true, "someTooMuch")
      """,
      "could not find implicit value for parameter runIdOperation:.+"
    )
  }
}
