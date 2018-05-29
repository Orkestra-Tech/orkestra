package com.goyeau.orchestra.github

import akka.http.scaladsl.model.Uri

import com.goyeau.orchestra.OrchestraConfig

case class GithubConfig(uri: Uri, port: Int, token: String)

object GithubConfig {
  def fromEnvVars() = GithubConfig(
    fromEnvVar("URI").fold(throw new IllegalStateException("ORCHESTRA_GITHUB_URI should be set"))(Uri(_)),
    fromEnvVar("PORT").map(_.toInt).getOrElse(8081),
    fromEnvVar("TOKEN").getOrElse(throw new IllegalStateException("ORCHESTRA_GITHUB_TOKEN should be set"))
  )

  def fromEnvVar(envVar: String) = OrchestraConfig.fromEnvVar(s"GITHUB_$envVar")
}
