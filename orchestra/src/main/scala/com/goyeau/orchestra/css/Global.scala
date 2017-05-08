package com.goyeau.orchestra.css

import scalacss.DevDefaults._
import scalacss.ProdDefaults._

object Global {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    style(
      unsafeRoot("body")(
        margin.`0`,
        padding.`0`,
        fontSize(14.px),
        fontFamily(fontFace("Roboto, sans-serif")(_.src("")))
      )
    )
  }
}
