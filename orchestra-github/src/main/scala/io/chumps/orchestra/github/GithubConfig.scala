package io.chumps.orchestra.github

import akka.http.scaladsl.model.Uri

import io.chumps.orchestra.OrchestraConfig

object GithubConfig {
  def apply(envVar: String) = OrchestraConfig(s"GITHUB_$envVar")

  lazy val url =
    OrchestraConfig("URL").fold(throw new IllegalStateException("ORCHESTRA_GITHUB_URL should be set"))(Uri(_))
  lazy val port = GithubConfig("PORT").map(_.toInt).getOrElse(81)
  lazy val token =
    GithubConfig("TOKEN").getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_TOKEN should be set"))
}
