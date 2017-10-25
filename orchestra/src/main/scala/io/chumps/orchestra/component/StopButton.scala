package io.chumps.orchestra.component

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

import autowire._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.ScalaCssReact._

import io.chumps.orchestra.css.Global
import io.chumps.orchestra.model.RunInfo

object StopButton {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val redButton = style(
      Global.Style.button,
      &.hover(backgroundColor.firebrick)
    )
  }

  val component = ScalaComponent
    .builder[RunInfo](getClass.getSimpleName)
    .render_P { runInfo =>
      <.div(Style.redButton, ^.width := "22px", ^.height := "22px", ^.onClick ==> stop(runInfo))("x")
    }
    .build

  private def stop(runInfo: RunInfo)(event: ReactEventFromInput) = Callback.future {
    event.stopPropagation()
    runInfo.job.Api.client.stop(runInfo.runId).call().map(Callback(_))
  }
}
