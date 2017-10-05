package io.chumps.orchestra.component

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.DevDefaults._
import scalacss.ProdDefaults._
import scalacss.ScalaCssReact._

import io.chumps.orchestra.model.{ActionMenu, PageMenu}
import io.chumps.orchestra.route.WebRouter.{AppPage, BoardPage, Status}

object TopNav {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val navMenu = style(backgroundColor(c"#F2706D"), margin.`0`, padding.`0`)

    val menuItem = styleF.bool { selected =>
      styleS(
        padding(20.px),
        fontSize(1.5.em),
        display.inlineBlock,
        cursor.pointer,
        mixinIfElse(selected)(backgroundColor(c"#E8433F"), fontWeight._500)(&.hover(backgroundColor(c"#B6413E")))
      )
    }
  }

  case class Props(rootBoard: BoardPage, selectedPage: AppPage, ctl: RouterCtl[AppPage])

  implicit val currentPageReuse = Reusability.by_==[AppPage]
  implicit val propsReuse = Reusability.by((_: Props).selectedPage)

  val component = ScalaComponent
    .builder[Props](getClass.getSimpleName)
    .render_P { props =>
      val leftMenu = Seq(
        PageMenu("Boards", props.rootBoard),
        PageMenu("Status", Status)
      )

      val rightMenu = Seq(
        ActionMenu("Running Jobs", Callback(()))
      )

      <.header(
        <.nav(^.display.block)(
          <.ul(Style.navMenu)(
            leftMenu.toTagMod { item =>
              <.li(
                ^.key := item.name,
                Style.menuItem(item.route.getClass == props.selectedPage.getClass),
                props.ctl.setOnClick(item.route)
              )(item.name)
            },
            rightMenu.toTagMod { item =>
              <.li(
                ^.key := item.name,
                Style.menuItem(false),
                ^.float.right,
                ^.onClick --> item.action
              )(item.name)
            }
          )
        )
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props) = component(props)

}
