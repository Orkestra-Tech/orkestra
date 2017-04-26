package com.goyeau.orchestra.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import com.goyeau.orchestra.models.Menu
import com.goyeau.orchestra.routes.AppRouter.AppPage

object TopNav {

  object Style extends StyleSheet.Inline {
    import dsl._

    val navMenu = style(display.flex, alignItems.center, backgroundColor(c"#F2706D"), margin.`0`, listStyle := "none")

    val menuItem = styleF.bool { selected =>
      styleS(
        padding(20.px),
        fontSize(1.5.em),
        cursor.pointer,
        color(c"rgb(244, 233, 233)"),
        mixinIfElse(selected)(backgroundColor(c"#E8433F"), fontWeight._500)(&.hover(backgroundColor(c"#B6413E")))
      )
    }
  }

  case class Props(menus: Vector[Menu], selectedPage: AppPage, ctrl: RouterCtl[AppPage])

  implicit val currentPageReuse = Reusability.by_==[AppPage]
  implicit val propsReuse = Reusability.by((_: Props).selectedPage)

  val component = ScalaComponent
    .builder[Props]("TopNav")
    .render_P { props =>
      <.header(
        <.nav(
          <.ul(
            Style.navMenu,
            props.menus.toTagMod { item =>
              <.li(
                ^.key := item.name,
                Style.menuItem(item.route.getClass == props.selectedPage.getClass),
                item.name,
                props.ctrl.setOnClick(item.route)
              )
            }
          )
        )
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(props: Props) = component(props)

}
