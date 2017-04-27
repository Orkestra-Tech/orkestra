package com.goyeau.orchestra

//import com.goyeau.orchestra.{Orchestra, WebServer}

object Main extends App {

  Orchestra("DriveTribe").startServer("localhost", 1234)

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
}
