/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkasse

import akka.stream.actor.{ ActorPublisher, ActorPublisherMessage }

/**
 * Base class for an actor publishing events. Concrete subclasses must implement [[receiveEvent]] which becomes part of
 * the initial behavior, i.e. gets called by the provided [[receive]] implementation. Typically `receiveEvent` would
 * be implemented simply be invoking [[onEvent]] which fist appends the received event (message) to the buffer which
 * is limited by the `bufferSize` parameter and then publishes the buffered events up to the total demand if this
 * publisher is active. `ActorPublisherMessage.Request` is handled by publishing the buffered events up to the requested
 * demand and other `ActorPublisherMessage`s (e.g. `Cancel`) stop this actor.
 * @param bufferSize the maximum number of events (messages) to be buffered
 */
abstract class EventPublisher[A: ToServerSentEvent](bufferSize: Int) extends ActorPublisher[ServerSentEvent] {

  private val toServerSentEvent = implicitly[ToServerSentEvent[A]]

  private var events = Vector.empty[A]

  /**
   * Receive events via [[onEvent]] and `ActorPublisherMessage`s.
   */
  final override def receive = receiveEvent.orElse {
    case ActorPublisherMessage.Request(demand) => publish(demand)
    case msg: ActorPublisherMessage            => context.stop(self)
  }

  /**
   * To be implemented by invoking [[onEvent]].
   */
  protected def receiveEvent: Receive

  /**
   * To be invoked when an event is received.
   */
  final protected def onEvent(event: A): Unit = {
    events = (events :+ event).takeRight(bufferSize)
    if (isActive) publish(totalDemand)
  }

  private def publish(demand: Long) = {
    val (requestedEvents, remainingEvents) = events.splitAt(demand.toInt)
    requestedEvents.foreach(toServerSentEvent.andThen(onNext))
    events = remainingEvents
  }
}
