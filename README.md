# Utilities for Akka

This package is published for both Scala 2.11 and 2.12 via bintray (soon JCenter). 

To use in an SBT project:
```$scala
resolvers in ThisBuild += Resolver.bintrayRepo("easel", "maven"),
libraryDependencies in ThisBuild += "io.github.easel" %% "utils-akka" % "0.0.1"
```

## ObservableBuffer

Adds a backpressuring buffer to an akka stream that publishes the current buffer
depth via a callback whenever it changes. Facilitates determining which stage(s) in 
a pipeline are the bottleneck. 

Usage:
```$scala
import akka.stream.scaladsl.{Sink, Source}
import io.github.easel.utils.akka.ObservableBuffer

Source.fromIterator(() => Range(0, 10).iterator)
    .via(ObservableBuffer[Int](3, (x: Int) => println(s"buffer depth $x")))
    .runWith(Sink.seq)
```

Please see the test cases in `ObservableBufferSpec` for
more details.
