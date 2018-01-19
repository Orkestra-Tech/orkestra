package io.chumps.orchestra.github

import io.chumps.orchestra.OrchestraConfig

object GithubConfig {
  def apply(envVar: String) = OrchestraConfig(s"GITHUB_$envVar")

  lazy val githubPort = GithubConfig("PORT")
    .map(_.toInt)
    .getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_PORT should be set"))
  lazy val githubToken =
    GithubConfig("TOKEN").getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_TOKEN should be set"))
}
