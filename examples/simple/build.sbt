lazy val orchestration = OrchestraProject("orchestration", file("orchestration"))
lazy val orchestrationJVM = orchestration.jvm
lazy val orchestrationJS = orchestration.js