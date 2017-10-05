package io.chumps.orchestra

case class Page[From](from: Option[From], size: Int)
