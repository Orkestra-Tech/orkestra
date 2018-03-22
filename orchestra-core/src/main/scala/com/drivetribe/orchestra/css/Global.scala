package com.drivetribe.orchestra.css

object Global {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val brandColor = c"#3570e5"

    val listItem = styleF.bool { pair =>
      styleS(
        mixinIf(pair)(boxShadow := "inset 0 0 10000px rgba(255, 255, 255, 0.02)"),
        &.hover(boxShadow := "inset 0 0 10000px rgba(255, 255, 255, 0.04)")
      )
    }

    val runId = style(width(280.px))

    val cell = style(display.flex, alignItems.center, padding(4.px), height(22.px))

    val button = style(display.flex, alignItems.center, justifyContent.center, cursor.pointer)

    val brandColorButton = style(
      button,
      &.hover(backgroundColor(Global.Style.brandColor))
    )

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
