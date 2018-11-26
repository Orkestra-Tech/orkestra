//package tech.orkestra.integration.tests.utils
//
//import scala.concurrent.Future
//import cats.implicits._
//import cats.effect.{ConcurrentEffect, IO, Sync}
//import com.goyeau.kubernetes.client.KubernetesClient
//import io.circe.{Decoder, Encoder, Json}
//import io.circe.parser._
//import io.circe.syntax._
//import org.http4s.{MediaType, Method}
//import org.http4s.dsl.impl.Path
//import org.http4s.headers.`Content-Type`
//import tech.orkestra.OrkestraConfig
//import tech.orkestra.utils.AkkaImplicits._
//
//object AutowireClient {
//
//  def apply[F[_]: ConcurrentEffect](kubernetesClient: KubernetesClient[F], segment: String) =
//    new autowire.Client[Json, Decoder, Encoder] {
//      override def doCall(request: Request): Future[Json] =
//        ConcurrentEffect[F]
//          .toIO(
//            kubernetesClient.services
//              .namespace(Kubernetes.namespace.metadata.get.name.get)
//              .proxy(
//                Deployorkestra.service.metadata.get.name.get,
//                Method.POST,
//                Path(s"/${(OrkestraConfig.apiSegment +: segment +: request.path).mkString("/")}"),
//                `Content-Type`(MediaType.application.json),
//                Option(request.args.asJson.noSpaces)
//              )
//              .map(raw => parse(raw).fold(throw _, identity))
//          )
//          .unsafeToFuture()
//
//      override def read[T: Decoder](json: Json) = json.as[T].fold(throw _, identity)
//      override def write[T: Encoder](obj: T) = obj.asJson
//    }
//}
