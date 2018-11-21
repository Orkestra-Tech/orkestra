package tech.orkestra.component

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import tech.orkestra.board.JobBoard
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import shapeless.HList
import tech.orkestra.css.Global
import tech.orkestra.model.RunId

object StopButton {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val redButton = style(
      Global.Style.button,
      &.hover(backgroundColor.firebrick)
    )
  }

  case class Props(job: JobBoard[_ <: HList], runId: RunId)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .render_P { props =>
      <.div(Style.redButton, ^.width := "30px", ^.height := "30px", ^.onClick ==> stop(props.job, props.runId))("x")
    }
    .build

  private def stop(job: JobBoard[_], runId: RunId)(event: ReactEventFromInput) = Callback.future {
    event.stopPropagation()
    job.Api.client.stop(runId).call().map(Callback(_))
  }
}
