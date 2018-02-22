import io.chumps.orchestra.AsyncDsl._
import io.chumps.orchestra.{Orchestra, UI}
import io.chumps.orchestra.board.{Folder, Job}
import io.chumps.orchestra.job.JobRunner
import io.chumps.orchestra.model.JobId

object Orchestration extends Orchestra with UI {
  lazy val board = Folder("My amazing company")(helloJob)
  lazy val jobRunners = Set(helloJobRunner)

  lazy val helloJob = Job[() => Unit](JobId("helloWorld"), "Hello World")()
  lazy val helloJobRunner = JobRunner(helloJob) { implicit workDir => () =>
    println("Hello World")
  }
}
