package io.github.easel.utils.akka

import java.io.File

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.MustMatchers

class WalkableTest extends org.scalatest.AsyncWordSpec with MustMatchers{
  "the java file worker" should {
    "walk" in {
      implicit val system: ActorSystem = ActorSystem()
      implicit val mat: Materializer = ActorMaterializer()
      val resultFut = Source.single(new File("/tmp")).via(Walkable.walk(true)).runWith(Sink.seq)
      resultFut.flatMap(r => system.whenTerminated.map(_ => r mustEqual ""))
    }
  }

}
