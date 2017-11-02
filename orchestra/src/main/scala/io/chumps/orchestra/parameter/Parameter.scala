package io.chumps.orchestra.parameter

import enumeratum._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ReactEventFromInput}

trait Parameter[T] {
  lazy val id: Symbol = Symbol(name.toLowerCase.replaceAll("\\s", ""))
  def name: String
  def defaultValue: Option[T]

  def display(state: State) = TagMod()
  def getValue(valueMap: Map[Symbol, Any]): T =
    valueMap
      .get(id)
      .map(_.asInstanceOf[T])
      .orElse(defaultValue)
      .getOrElse(throw new IllegalArgumentException(s"Can't get param ${id.name}"))
}

case class State(updated: ((Symbol, Any)) => Callback, get: Symbol => Option[Any]) {
  def +(kv: (Symbol, Any)) = updated(kv)
}

case class Input[T: Encoder: Decoder](name: String, defaultValue: Option[T] = None) extends Parameter[T] {
  override def display(state: State) = {
    def modValue(event: ReactEventFromInput) = {
      event.persist()
      state + (id -> implicitly[Encoder[T]].apply(event.target.value))
    }

    <.label(^.display.block)(
      <.span(name),
      <.input.text(
        ^.key := id.name,
        ^.value := state.get(id).map(_.asInstanceOf[T]).orElse(defaultValue).fold("")(implicitly[Decoder[T]].apply(_)),
        ^.onChange ==> modValue
      )
    )
  }
}

case class Checkbox(name: String, checked: Boolean = false) extends Parameter[Boolean] {
  def defaultValue = Option(checked)

  override def display(state: State) = {
    def modValue(event: ReactEventFromInput) = {
      event.persist()
      state + (id -> event.target.checked)
    }

    <.label(^.display.block)(
      <.input.checkbox(
        ^.key := id.name,
        ^.checked := state.get(id).map(_.asInstanceOf[Boolean]).orElse(defaultValue).getOrElse(false),
        ^.onChange ==> modValue
      ),
      <.span(name)
    )
  }
}

case class Select[Entry <: EnumEntry](name: String, enum: Enum[Entry], defaultValue: Option[Entry] = None)
    extends Parameter[Entry] {
  override def display(state: State) = {
    def modValue(event: ReactEventFromInput) = {
      event.persist()
      state + (id -> enum.withNameInsensitive(event.target.value))
    }
    val disabled = "disabled"

    <.label(^.display.block)(
      <.span(name),
      <.select(
        ^.key := id.name,
        ^.value := state.get(id).map(_.asInstanceOf[Entry]).orElse(defaultValue).map(_.entryName).getOrElse(disabled),
        ^.onChange ==> modValue
      )(
        <.option(^.disabled := true, ^.value := disabled)(name) +:
          enum.values.map(o => <.option(^.value := o.entryName)(o.toString)): _*
      )
    )
  }
}
