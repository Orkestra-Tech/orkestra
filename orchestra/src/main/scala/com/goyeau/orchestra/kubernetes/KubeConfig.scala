package com.goyeau.orchestra.kubernetes

import com.goyeau.orchestra.Config

object KubeConfig {
  val uri = Config("KUBE_URI").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_URI should be set"))
  val podName =
    Config("KUBE_POD_NAME").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_POD_NAME should be set"))
  val namespace =
    Config("KUBE_NAMESPACE").getOrElse(throw new IllegalStateException("ORCHESTRA_KUBE_NAMESPACE should be set"))
}
