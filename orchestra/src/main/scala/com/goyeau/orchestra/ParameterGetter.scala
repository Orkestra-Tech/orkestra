package com.goyeau.orchestra

trait ParameterGetter[T, P <: Parameter[T]] {
  def apply(param: P, valueMap: Map[Symbol, Any]): T
}

object ParameterGetter extends LowPriorityGetters {

  implicit val booleanValue = new ParameterGetter[Boolean, Param[Boolean]] {
    def apply(param: Param[Boolean], valueMap: Map[Symbol, Any]) =
      valueMap.get(param.id).fold(false)(_.isInstanceOf[Boolean])
  }
}

trait LowPriorityGetters {

  implicit def default[T, P <: Parameter[T]] = new ParameterGetter[T, P] {
    def apply(param: P, valueMap: Map[Symbol, Any]) =
      valueMap
        .get(param.id)
        .map(_.asInstanceOf[T])
        .orElse(param.defaultValue)
        .getOrElse(throw new IllegalArgumentException(s"Can't get param ${param.id.name}"))
  }
}
