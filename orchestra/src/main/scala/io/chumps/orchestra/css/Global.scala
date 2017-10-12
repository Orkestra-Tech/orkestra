package io.chumps.orchestra.css

import scalacss.DevDefaults._
import scalacss.ProdDefaults._

object Global {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val listItem = styleF.bool { pair =>
      styleS(
        mixinIf(pair)(backgroundColor(c"rgba(255, 255, 255, 0.02)")),
        &.hover(backgroundColor(c"rgba(255, 255, 255, 0.04)"))
      )
    }

    style(
      unsafeRoot("body")(
        backgroundColor(c"#2b2b2b"),
        color(c"#f4f4f4"),
        margin.`0`,
        padding.`0`,
        fontSize(14.px),
        fontFamily(fontFace("Roboto, sans-serif")(_.src("")))
      )
    )
  }
}
