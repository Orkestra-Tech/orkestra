import com.drivetribe.orchestra._
import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.board._
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model._

object Orchestration extends Orchestra with UI {
  lazy val board = Folder("Orchestra")(helloJob)
  lazy val jobRunners = Set(helloJobRunner)

  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
    println("Hello World")
  }
}
