package com.drivetribe.orchestra

import scala.language.implicitConversions

import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import com.drivetribe.orchestra.utils._

trait AutoTuple1 {
  implicit def autoTuple1[T](o: T): Tuple1[T] = Tuple1(o)
}

object Dsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with AkkaImplicits
