package com.goyeau

import io.circe.shapes.HListInstances
import io.circe.generic.AutoDerivation

package object orchestra extends HListInstances with AutoDerivation
