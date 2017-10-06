package io.github.easel.utils.akka

import better.files._
import java.io.{File => JFile}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}

trait Walkable[T] {
  def branches(t: T): Source[T, akka.NotUsed]
  def leaves(t: T): Source[T, akka.NotUsed]
}

object Walkable {
  implicit val fileWalkable: Walkable[JFile] {
    def leaves(t: JFile): Source[JFile, akka.NotUsed]

    def branches(t: JFile): Source[JFile, akka.NotUsed]
  } = new Walkable[JFile] {
    override def branches(t: JFile): Source[JFile, akka.NotUsed] =
      Source.fromIterator(() =>
        t.toScala.children.filter(_.isDirectory).map(_.toJava))

    override def leaves(t: JFile): Source[JFile, akka.NotUsed] =
      Source.fromIterator(() =>
        t.toScala.children.filter(_.isRegularFile).map(_.toJava))
  }

  def walk[T](depthFirst: Boolean = true)(
      implicit walkable: Walkable[T]): Flow[T, T, NotUsed] =
    Flow[T]
      .flatMapConcat { w =>
        if (depthFirst) walkable.branches(w).via(walk(depthFirst)(walkable))
        else walkable.leaves(w)
      }
}
