package tech.orkestra

import scala.language.implicitConversions

import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import tech.orkestra.utils._

object Dsl extends HListInstances with AutoDerivation with AkkaImplicits {
  implicit def autoTuple1[T](o: T): Tuple1[T] = Tuple1(o)
}
