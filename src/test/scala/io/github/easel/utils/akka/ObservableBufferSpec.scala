package io.github.easel.utils.akka

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class ObservableBufferSpec
    extends TestKit(ActorSystem("ObservableBufferSpec"))
    with ScalaFutures
    with ImplicitSender
    with IntegrationPatience
    with WordSpecLike
    with MustMatchers
    with BeforeAndAfterAll {

  implicit lazy val mat: Materializer = ActorMaterializer()
  implicit lazy val ec: ExecutionContext = system.dispatcher

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "the observable buffer" should {
    "fire observations whenever the buffer is used" in {
      var observations = List.empty[Int]

      val observer: Int => Unit =
        (depth: Int) =>
          synchronized {
            observations :+= depth
        }

      Source
        .fromIterator(() => Range(0, 10).iterator)
        .via(ObservableBuffer[Int](3, observer))
        .throttle(1, 1.millis, 1, ThrottleMode.shaping)
        .runWith(Sink.seq)
        .futureValue
        .length mustEqual 10

      observations mustEqual List(0, 1, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 1, 0)
    }
  }
  "minimal usage" in {
    Source
      .fromIterator(() => Range(0, 10).iterator)
      .via(ObservableBuffer[Int](3, (x: Int) => println(s"$x")))
      .runWith(Sink.seq)
      .futureValue
      .length mustEqual 10

  }

}
