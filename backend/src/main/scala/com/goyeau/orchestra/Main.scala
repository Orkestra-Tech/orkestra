package com.goyeau.orchestra

//import com.goyeau.orchestra.{Orchestra, WebServer}

object Main extends App {

  Orchestra("DriveTribe").startServer("localhost", 1234)
}
