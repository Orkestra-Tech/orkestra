package tech.orkestra.model

case class Page[T](after: Option[T], size: Int)
