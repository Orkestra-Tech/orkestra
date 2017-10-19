package io.chumps.orchestra.model

case class Page[From](from: Option[From], size: Int)
