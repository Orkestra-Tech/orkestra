package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.Config

object KubeConfig {
  val kubeHost =
    Config("ORCHESTRA_KUBE_HOST").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_HOST should be set"))
}
