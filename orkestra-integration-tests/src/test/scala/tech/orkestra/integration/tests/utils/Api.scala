//package tech.orkestra.integration.tests.utils
//
//import tech.orkestra.board.JobBoard
//import shapeless.HList
//import tech.orkestra.{CommonApi, OrkestraConfig}
//
//object Api {
//  def jobClient[Parameters <: HList, Result](job: JobBoard[Parameters]) =
//    AutowireClient(Kubernetes.client, s"${OrkestraConfig.jobSegment}/${job.id.value}")[job.Api]
//
//  val commonClient = AutowireClient(Kubernetes.client, OrkestraConfig.commonSegment)[CommonApi]
//}
