---
layout: docs
title:  "Locking"
position: 12
---

# Locking

Orchestra can ensure some code is running only once at a time across all the running jobs via the `orchestra-lock`
library/plugin. We can do so by adding the dependency in `build.sbt`:
```scala
libraryDependencies += "com.drivetribe" %% "orchestra-lock" % orchestraVersion
```

Then we can use the `Lock` class to create a lock and use it either awaiting the potential release of the lock:
```tut:silent
import com.drivetribe.orchestra.lock.Lock

def deploy(environment: String) = Lock(environment).orWait {
  println(s"Deploying on $environment")
}
```

Or by running another function if the lock is already acquired:
```tut:silent
import com.drivetribe.orchestra.lock.Lock

def deploy(environment: String) = Lock(environment).orElse {
  println(s"Deploying on $environment")
}(throw new IllegalStateException("A deployment is already going on right now"))
```
