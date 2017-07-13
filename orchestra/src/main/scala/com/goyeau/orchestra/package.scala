package com.goyeau

import com.goyeau.orchestra.io.DirectoryHelpers
import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

package object orchestra extends HListInstances with AutoDerivation with DirectoryHelpers
