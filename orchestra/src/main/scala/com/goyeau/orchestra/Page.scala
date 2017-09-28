package com.goyeau.orchestra

case class Page[From](from: Option[From], size: Int)
