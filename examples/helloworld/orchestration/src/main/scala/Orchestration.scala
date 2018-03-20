import com.drivetribe.orchestra.AsyncDsl._
import com.drivetribe.orchestra.{Orchestra, UI}
import com.drivetribe.orchestra.board.{Folder, Job}
import com.drivetribe.orchestra.job.JobRunner
import com.drivetribe.orchestra.model.JobId

object Orchestration extends Orchestra with UI {
  lazy val board = Folder("My amazing company")(helloJob)
  lazy val jobRunners = Set(helloJobRunner)

  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
    println("Hello World")
  }
}
