package com.goyeau.orkestra.model

import java.time.Instant

import shapeless.HList

import com.goyeau.orkestra.model.Indexed._

case class History[ParamValues <: HList, Result](runs: Seq[(Run[ParamValues, Result], Seq[Stage])], updatedOn: Instant)
