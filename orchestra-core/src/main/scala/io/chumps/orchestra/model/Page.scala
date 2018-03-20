package com.drivetribe.orchestra.model

case class Page[T](after: Option[T], size: Int)
