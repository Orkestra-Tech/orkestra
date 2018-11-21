package tech.orkestra.model

import java.time.Instant

import io.circe.{Decoder, Encoder}
import shapeless.HList
import tech.orkestra.model.Indexed._

case class History[Parameters <: HList](runs: Seq[(Run[Parameters, _], Seq[Stage])], updatedOn: Instant)

object History {
  implicit def encoder[Parameters <: HList]: Encoder[History[Parameters]] = ???
  implicit def decoder[Parameters <: HList]: Decoder[History[Parameters]] = ???
}
