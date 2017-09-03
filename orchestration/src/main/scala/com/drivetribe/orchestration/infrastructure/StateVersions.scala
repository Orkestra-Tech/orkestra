package com.drivetribe.orchestration.infrastructure

object StateVersions {

  def template(stateVersion: String) =
    s"'{'state_versions':{'default': $stateVersion,'userstats': $stateVersion}}'"
}
