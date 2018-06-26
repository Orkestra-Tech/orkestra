package tech.orkestra.model

import java.time.Instant

import shapeless.HList

import tech.orkestra.model.Indexed._

case class History[ParamValues <: HList, Result](runs: Seq[(Run[ParamValues, Result], Seq[Stage])], updatedOn: Instant)
