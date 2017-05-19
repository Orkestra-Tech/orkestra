import java.time.Instant

trait Encoder[T] {
  def apply(o: T): String
}

object Encoder {
  def apply[T: Encoder](o: T): String = implicitly[Encoder[T]].apply(o)

  implicit val stringEncoder = new Encoder[String] {
    override def apply(o: String): String = o
  }

  implicit val intEncoder = new Encoder[Int] {
    override def apply(o: Int): String = o.toString
  }

  implicit val instantEncoder = new Encoder[Instant] {
    override def apply(o: Instant): String = o.toEpochMilli.toString
  }
}

// Simple example
SomeSevice.sendMessage(Instant.now())

// Example showing how you can easily extend it
case class Person(name: String, age: Int, birth: Instant)

object ExtraEncoder {

  implicit def personEncoder(implicit string: Encoder[String], int: Encoder[Int], instant: Encoder[Instant]) =
    new Encoder[Person] {
      override def apply(o: Person): String =
        s"""{
           |  name: ${Encoder(o.name)}
           |  age: ${Encoder(o.age)}
           |  birth: ${Encoder(o.birth)}
           |}
           |""".stripMargin
    }
}

object SomeSevice {
  def sendMessage[T: Encoder](message: T) = println(s"Sending: ${Encoder(message)}")
}

import ExtraEncoder._
SomeSevice.sendMessage(Person("Damien", 27, Instant.now()))