package io.github.easel.utils.akka

import akka.event.Logging
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

object ObservableBuffer {

  /**
    * Forked from akka-internals
    * @tparam T
    */
  private[akka] abstract class SimpleLinearGraphStage[T]
      extends GraphStage[FlowShape[T, T]] {
    val in = Inlet[T](Logging.simpleName(this) + ".in")
    val out = Outlet[T](Logging.simpleName(this) + ".out")
    override val shape = FlowShape(in, out)
  }
}

/**
  * A simple observable buffer stage.
  * Fires the observer whenever the buffer depth changes.
  *
  * Usage:
  * Source.via(ObservableBuffer[Int](3, (x: Int) => println(s"buffer depth $x")))
  *
  * @param size
  * @param bufferObserver
  * @tparam T
  */
final case class ObservableBuffer[T](size: Int, bufferObserver: Int => Unit)
    extends ObservableBuffer.SimpleLinearGraphStage[T] {

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      private var buffer: Buffer[T] = _

      val enqueueAction: T ⇒ Unit =
        elem ⇒ {
          buffer.enqueue(elem)
          bufferObserver(buffer.used)
          if (!buffer.isFull) pull(in)
        }

      override def preStart(): Unit = {
        buffer = Buffer(size, materializer)
        bufferObserver(buffer.used)
        pull(in)
      }

      override def onPush(): Unit = {
        val elem = grab(in)
        // If out is available, then it has been pulled but no dequeued element has been delivered.
        // It means the buffer at this moment is definitely empty,
        // so we just push the current element to out, then pull.
        if (isAvailable(out)) {
          push(out, elem)
          pull(in)
        } else {
          enqueueAction(elem)
        }
      }

      override def onPull(): Unit = {
        if (buffer.nonEmpty) {
          push(out, buffer.dequeue())
          bufferObserver(buffer.used)
        }
        if (isClosed(in)) {
          if (buffer.isEmpty) completeStage()
        } else if (!hasBeenPulled(in)) {
          pull(in)
        }
      }

      override def onUpstreamFinish(): Unit = {
        if (buffer.isEmpty) completeStage()
      }

      setHandlers(in, out, this)
    }

}
