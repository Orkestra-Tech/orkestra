package com.drivetribe.orchestration

import com.goyeau.orchestra.filesystem.{Directory, LocalFile}
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.api.{Git => JGit}

object Git {

  def checkoutInfrastructure()(implicit workDir: Directory) = {
    val git = JGit
      .cloneRepository()
      .setURI(s"https://github.com/drivetribe/infrastructure.git")
      .setCredentialsProvider(
        new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_USERNAME"), System.getenv("GITHUB_TOKEN"))
      )
      .setDirectory(LocalFile("infrastructure"))
      .setNoCheckout(true)
      .call()
    git.getRepository.getConfig.setBoolean("core", null, "fileMode", true)
    git.checkout().setName("origin/master").call()
  }
}
