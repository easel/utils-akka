package io.github.easel.utils.akka

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, Stash}
import akka.pattern.pipe

import scala.concurrent.Future

/**
  * Abstract out the boilerplate of initializing a no-parameter actor on
  * actor startup.
  */
object AsyncInitialized {

  /**
    * The Initializer typeclass provides the dependencies via a future. Analogous
    * to a constructor but asynchronous.
    * @tparam T
    */
  abstract class Initializer[T] {
    def initialize: Future[T]
  }

  case class InitializationFailed(t: Throwable)
}

trait AsyncInitialized[T] extends Stash with ActorLogging {
  import AsyncInitialized._
  import context._

  /**
    * Carrier class for the successfully initialized dependencies
    * @param dependencies
    */
  private case class Initialized(dependencies: T)

  /**
    * Start the initializer and forward the results to ourself
    */
  override def preStart(): Unit = initializer.initialize.map(Initialized) pipeTo self

  /**
    * Do not override the default receive, use become in your running()
    * @return
    */
  def receive: Receive = uninitialized

  /**
    * Default initial state, waits for the initializer to come back and passes into
    * to the running state.
    * @return
    */
  def uninitialized: Receive = {
    case Initialized(dependencies) =>
      log.debug(s"Initialized ${getClass.getSimpleName}, replaying stash")
      unstashAll()
      become(initialized(dependencies))
    case Failure(e) =>
      log.warning(s"Failed to initialize ${getClass.getSimpleName}", e)
      context.parent ! InitializationFailed(e)
      context.stop(self)
    case msg =>
      log.debug(s"Received $msg while initializing, stashing")
      stash()
  }

  /**
    * Initializer typeclass, provides a Future[T] via the initialize method
    * @return
    */
  def initializer: Initializer[T]

  /**
    * The entrypoint for the initialized actor
    * @param dependencies
    * @return
    */
  def initialized(dependencies: T): Receive
}
