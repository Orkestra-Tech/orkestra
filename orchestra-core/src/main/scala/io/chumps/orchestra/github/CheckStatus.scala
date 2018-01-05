package io.chumps.orchestra.github

import io.circe.{Encoder, Json}

case class CheckStatus(state: State, target_url: String, description: String, context: String)

sealed trait State { val description: String }
object State {
  case object Pending extends State {
    val description = "Check running for this commit"
  }
  case object Success extends State {
    val description = "Check passed for this commit"
  }
  case object Failure extends State {
    val description = "Check failed for this commit"
  }

  implicit val stateEncoder: Encoder[State] = o =>
    Json.fromString(o.getClass.getSimpleName.toLowerCase.replace("$", ""))
}
