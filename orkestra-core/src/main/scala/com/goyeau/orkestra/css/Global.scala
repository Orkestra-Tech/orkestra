package com.goyeau.orkestra.css

object Global {
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._

  object Style extends StyleSheet.Inline {
    import dsl._

    val brandKubernetesColor = c"#3570E5"
    val brandScalaColor = c"#DA3435"

    val listItem = styleF.bool { pair =>
      styleS(
        mixinIf(pair)(boxShadow := "inset 0 0 10000px rgba(255, 255, 255, 0.02)"),
        &.hover(boxShadow := "inset 0 0 10000px rgba(255, 255, 255, 0.04)")
      )
    }

    val runId = style(width(310.px))

    val cell = style(display.flex, alignItems.center, padding(4.px), height(22.px))

    val button = style(display.flex, alignItems.center, justifyContent.center, cursor.pointer)

    val brandColorButton = style(
      button,
      &.hover(backgroundColor(Global.Style.brandKubernetesColor))
    )

    val header = style(
      position.fixed,
      top.`0`,
      width(100.%%)
    )

    val main = style(
      paddingTop(70.px),
      paddingBottom(70.px)
    )

    val footer = style(
      position.absolute,
      backgroundColor(brandKubernetesColor),
      bottom.`0`,
      width(100.%%),
      textAlign.center
    )

    style(
      unsafeRoot("html")(
        height(100.%%)
      ),
      unsafeRoot("body")(
        position.relative,
        backgroundColor(c"#2b2b2b"),
        color(c"#f4f4f4"),
        fontFamily(fontFace("Roboto, sans-serif")(_.src(""))),
        margin.`0`,
        minHeight(100.%%)
      ),
      unsafeRoot("::selection")(
        backgroundColor(brandScalaColor)
      )
    )
  }
}
