package io.chumps

import io.chumps.orchestra.filesystem.DirectoryHelpers
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

import io.chumps.orchestra.utils.{ShellHelpers, StagesHelpers, TriggerHelpers}

package object orchestra
    extends HListInstances
    with AutoDerivation
    with DirectoryHelpers
    with ShellHelpers
    with TriggerHelpers
    with StagesHelpers {

  implicit def autoTuple1[T](o: T): Tuple1[T] = Tuple1(o)
}
