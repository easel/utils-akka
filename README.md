# Utilities for Akka

## ObservableBuffer

Adds a backpressuring buffer to an akka stream that publishes the current buffer
depth via a callback whenever it changes. Facilites determining which stage(s) in 
a pipeline are the bottleneck

Usage:
```$scala
import akka.stream.scaladsl.{Sink, Source}
import io.github.easel.utils.akka.ObservableBuffer

Source.fromIterator(() => Range(0, 10).iterator)
    .via(ObservableBuffer[Int](3, (x: Int) => println(s"buffer depth $x")))
    .runWith(Sink.seq)
```
