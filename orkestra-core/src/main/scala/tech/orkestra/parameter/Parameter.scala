package tech.orkestra.parameter

import enumeratum._
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ReactEventFromInput}

/**
  * A parameter is a UI element for user to parametrise the run of a job.
  */
trait Parameter[T] {
  lazy val id: Symbol = Symbol(name.toLowerCase.replaceAll("\\s", ""))
  def name: String
  def default: Option[T]

  def display(state: State) = TagMod()
  def getValue(valueMap: Map[Symbol, Any]): T =
    valueMap
      .get(id)
      .map(_.asInstanceOf[T])
      .orElse(default)
      .getOrElse(throw new IllegalArgumentException(s"Can't get param ${id.name}"))
}

case class State(updated: ((Symbol, Any)) => Callback, get: Symbol => Option[Any]) {
  def +(kv: (Symbol, Any)) = updated(kv)
}

/**
  * An input field where the user can enter data.
  */
case class Input[T: Encoder: Decoder](name: String, default: Option[T] = None) extends Parameter[T] {
  override def display(state: State) = {
    def modValue(event: ReactEventFromInput) = {
      event.persist()
      state + (id -> implicitly[Decoder[T]].apply(event.target.value))
    }

    <.label(^.display.block)(
      <.span(name),
      <.input.text(
        ^.key := id.name,
        ^.value := state.get(id).map(_.asInstanceOf[T]).orElse(default).fold("")(implicitly[Encoder[T]].apply(_)),
        ^.onChange ==> modValue
      )
    )
  }
}

/**
  * A checkbox.
  */
case class Checkbox(name: String, checked: Boolean = false) extends Parameter[Boolean] {
  def default = Option(checked)

  override def display(state: State) = {
    def modValue(event: ReactEventFromInput) = {
      event.persist()
      state + (id -> event.target.checked)
    }

    <.label(^.display.block)(
      <.input.checkbox(
        ^.key := id.name,
        ^.checked := state.get(id).map(_.asInstanceOf[Boolean]).orElse(default).getOrElse(false),
        ^.onChange ==> modValue
      ),
      <.span(name)
    )
  }
}

/**
  * A drop-down list.
  */
case class Select[Entry <: EnumEntry](name: String, enum: Enum[Entry], default: Option[Entry] = None)
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
        ^.value := state.get(id).map(_.asInstanceOf[Entry]).orElse(default).map(_.entryName).getOrElse(disabled),
        ^.onChange ==> modValue
      )(
        <.option(^.disabled := true, ^.value := disabled)(name) +:
          enum.values.map(o => <.option(^.value := o.entryName)(o.toString)): _*
      )
    )
  }
}
