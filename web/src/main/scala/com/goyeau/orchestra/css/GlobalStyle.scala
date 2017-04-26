package com.goyeau.orchestra.css

import scalacss.Defaults._

object GlobalStyle extends StyleSheet.Inline {

  import dsl._

  style(
    unsafeRoot("body")(
      margin.`0`,
      padding.`0`,
      fontSize(14.px),
      fontFamily := "Roboto, sans-serif"
    )
  )
}
