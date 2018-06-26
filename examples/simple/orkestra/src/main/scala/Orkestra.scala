import tech.orkestra._
import tech.orkestra.Dsl._
import tech.orkestra.board._
import tech.orkestra.job._
import tech.orkestra.model._

object Orkestra extends OrkestraServer {
  // Configuring the UI
  lazy val board = deployFrontendJobBoard
  // Configuring the jobs
  lazy val jobs = Set(deployFrontendJob)

  // Creating the job and configuring UI related settings
  lazy val deployFrontendJobBoard = JobBoard[() => Unit](JobId("deployFrontend"), "Deploy Frontend")()
  // Creating the job from the above definition (this will be compiled to JVM)
  lazy val deployFrontendJob = Job(deployFrontendJobBoard) { implicit workDir => () =>
    println("Deploying Frontend")
  }
}
