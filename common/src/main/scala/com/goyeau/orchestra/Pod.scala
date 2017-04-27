package com.goyeau.orchestra

case class Pod(containers: String*)(f: Seq[String] => Unit) {}

//Pod(Aws) { aws =>
//  aws.
//}
