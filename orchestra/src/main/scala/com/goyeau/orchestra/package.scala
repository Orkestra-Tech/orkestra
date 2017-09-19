package com.goyeau

import com.goyeau.orchestra.filesystem.DirectoryHelpers
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

package object orchestra
    extends HListInstances
    with AutoDerivation
    with DirectoryHelpers
    with ShellHelpers
    with TriggerHelpers
    with LoggingHelpers
