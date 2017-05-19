package com.goyeau.orchestra.kubernetes

case class Pod(containers: String*)(f: Seq[String] => Unit) {}

//Pod(Aws) { aws =>
//  aws.
//}
