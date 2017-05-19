package com.goyeau.orchestra.kubernetes

case class KubeContainer(name: String, image: String, command: Seq[String])

case class KubeJob(name: String, containers: Seq[KubeContainer], restartPolicy: String)

//object KubeJob {
//  def schedule(job: KubeJob) =
//}
