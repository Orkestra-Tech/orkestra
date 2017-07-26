package com.drivetribe.orchestration

object StateVersions {

  def template(version: String) =
    s"'{'state_versions':{'default': $version,'userstats': $version}}'"
}
