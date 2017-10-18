package io.github.easel.utils.akka

import akka.actor.Status.Failure
import akka.actor.{ActorLogging, Stash}
import akka.cluster.sharding.ShardRegion.{EntityId, ExtractEntityId}
import akka.pattern.pipe

import scala.concurrent.Future

/**
  * Abstraction for handling initialization of an actor on startup, given
  * an entity id. Generally, these will be actors configured as a part of a
  * `ShardRegion`.
  */
object AsyncInitializeShardEntity {

  /**
    * The initializer typeclass contains a reference to the extractEntityId method
    * used by the shard region, and can pass that id on to the initializer so it can load
    * all necessary state prior to transitioning to the running state.
    * @tparam T The dependencies
    */
  abstract class Initializer[T <: Dependencies] {
    def extractEntityId: ExtractEntityId
    def initialize(id: EntityId): Future[T]
  }

  /**
    * All dependencies must carry along with them the entityId of this actor. Allows
    * for more useful log messages.
    */
  trait Dependencies {
    def entityId: EntityId
  }

  /**
    * Captures the initialization failure so it can be sent back to the parent.
    *
    * @param throwable
    */
  case class InitializationFailed(throwable: Throwable)
}

trait AsyncInitializeShardEntity[T <: AsyncInitializeShardEntity.Dependencies]
    extends Stash
    with ActorLogging {
  import AsyncInitializeShardEntity._
  import context._

  /**
    * The internal message passed when the initialization succeeds
    * @param dependencies
    */
  private case class Initialized(dependencies: T)

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
      log.debug(s"Initialized for entity id ${dependencies.entityId}")
      unstashAll()
      become(initialized(dependencies))
    case Failure(e) =>
      log.debug(s"Failed to initialize ${getClass.getSimpleName}: ${e.getMessage}, shutting down.")
      context.stop(self)
    case msg =>
      if (initializer.extractEntityId.isDefinedAt(msg)) {
        val (entityId, _) = initializer.extractEntityId(msg)
        log.debug(s"Initializing for entity id $entityId")
        initializer.initialize(entityId).map(Initialized).pipeTo(self)
      } else {
        log.debug(s"Received $msg prior to initialization completion, stashing")
      }
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
