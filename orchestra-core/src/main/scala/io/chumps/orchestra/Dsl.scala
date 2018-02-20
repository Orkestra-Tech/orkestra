package io.chumps.orchestra

import scala.language.implicitConversions

import io.chumps.orchestra.filesystem.DirectoryUtils
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import io.chumps.orchestra.utils.{AsyncShellUtils, ShellUtils, StagesUtils, TriggerUtils}

trait AutoTuple1 {
  implicit def autoTuple1[T](o: T): Tuple1[T] = Tuple1(o)
}

object AsyncDsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with DirectoryUtils
    with TriggerUtils
    with StagesUtils
    with AsyncShellUtils

object Dsl
    extends HListInstances
    with AutoDerivation
    with AutoTuple1
    with DirectoryUtils
    with TriggerUtils
    with StagesUtils
    with ShellUtils
