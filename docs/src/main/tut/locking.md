---
layout: docs
title:  "Locking"
position: 12
---

# Locking

Orchestra can ensure some code is running only once at a time across all the running jobs via the `orchestra-lock`
library/plugin. We can do so by adding the dependency in `build.sbt`:
```scala
libraryDependencies += "com.drivetribe" %%% "orchestra-lock" % orchestraVersion
```

Documentation coming soon
