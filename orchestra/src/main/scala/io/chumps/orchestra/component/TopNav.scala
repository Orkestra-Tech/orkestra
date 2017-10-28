package io.chumps.orchestra.component

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.ScalaCssReact._

import shapeless.HList

import io.chumps.orchestra.board.Job
import io.chumps.orchestra.css.Global
import io.chumps.orchestra.route.WebRouter.{BoardPageRoute, PageRoute, StatusPageRoute}

object TopNav {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val navMenu = style(backgroundColor(Global.Style.brandColor), margin.`0`, padding.`0`, listStyle := "none")

    val menuItem = styleF.bool { selected =>
      styleS(
        padding(20.px),
        fontSize(1.5.em),
        display.inlineBlock,
        cursor.pointer,
        mixinIfElse(selected)(boxShadow := "inset 0 0 10000px rgba(0, 0, 0, 0.06)")(
          &.hover(boxShadow := "inset 0 0 10000px rgba(255, 255, 255, 0.06)")
        )
      )
    }
  }

  case class PageMenu(name: String, route: PageRoute)

  case class Props(rootPage: BoardPageRoute,
                   selectedPage: PageRoute,
                   ctl: RouterCtl[PageRoute],
                   jobs: Seq[Job[_, _ <: HList]])

  implicit val currentPageReuse: Reusability[PageRoute] = Reusability.by_==[PageRoute]
  implicit val propsReuse: Reusability[Props] = Reusability.by((_: Props).selectedPage)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .initialState[Boolean](false)
    .renderP { ($, props) =>
      val leftMenu = Seq(
        PageMenu("Boards", props.rootPage),
        PageMenu("Status", StatusPageRoute)
      )

      <.header(
        <.nav(^.display.block)(
          <.ul(Style.navMenu)(
            leftMenu.toTagMod { item =>
              <.li(
                Style.menuItem(item.route.getClass == props.selectedPage.getClass),
                props.ctl.setOnClick(item.route)
              )(item.name)
            },
            <.li(^.float.right,
                 ^.position.relative,
                 ^.tabIndex := 0,
                 ^.outline := "none",
                 ^.onBlur --> $.setState(false))(
              <.div(TopNav.Style.menuItem($.state), ^.onClick --> $.modState(!_))("Running Jobs"),
              if ($.state) RunningJobs.component(props.jobs) else TagMod()
            )
          )
        )
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

}
