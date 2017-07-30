package com.drivetribe.orchestration.infrastructure

object StateVersions {

  def template(version: String) =
    s"'{'state_versions':{'default': $version,'userstats': $version}}'"
}
