//package com.goyeau.orchestra
//
//object Test extends App {
//  import java.io._
//
//  def write[A](obj: A): Array[Byte] = {
//    val bo = new ByteArrayOutputStream()
//    new ObjectOutputStream(bo).writeObject(obj)
//    bo.toByteArray
//  }
//
//  def read(bytes: Array[Byte]): Any =
//    new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject()
//
//  read(write({ a: Int =>
//    a + 1
//  })).asInstanceOf[Function[Int, Int]](5) // == 6
//
//  // And case class serialization
//  case class XF(f: (Int => Int)) extends Serializable
//
//  val w = write(XF((a: Int) => a + 1))
//
//  println(new String(w))
//
//  val r = read(w).asInstanceOf[XF].f(5)
//
//  println(r)
//}
//  val channel = new RandomAccessFile(new File("lock"), "rw").getChannel
//  channel.lock() // Block
//  println("Locked")
//  Thread.sleep(10000)
//  println("Unlocked")
//  channel.close()

//  Orchestrateur()
//    .addJob(SwitchColour.switchColour)
//    .addScheduledJob("cron string" -> SwitchColour.switchColour)
//    .start()
//  val j2 = Job {
//    println("Hello")
//  }
//
//  Job.run(depoyBackend, Map("buildNumber" -> "12", "foo" -> "Result", "bar" -> "12", "baz" -> "2"))
//  Job.run(j2, Map("buildNumber" -> "12"))
