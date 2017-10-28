package io.chumps.orchestra.component

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.ScalaCssReact._

import shapeless.HList

import io.chumps.orchestra.board.Job
import io.chumps.orchestra.css.Global
import io.chumps.orchestra.model.RunId

object StopButton {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val redButton = style(
      Global.Style.button,
      &.hover(backgroundColor.firebrick)
    )
  }

  case class Props(job: Job[_, _ <: HList], runId: RunId)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .render_P { props =>
      <.div(Style.redButton, ^.width := "28px", ^.height := "28px", ^.onClick ==> stop(props.job, props.runId))("x")
    }
    .build

  private def stop(job: Job[_, _], runId: RunId)(event: ReactEventFromInput) = Callback.future {
    event.stopPropagation()
    job.Api.client.stop(runId).call().map(Callback(_))
  }
}
