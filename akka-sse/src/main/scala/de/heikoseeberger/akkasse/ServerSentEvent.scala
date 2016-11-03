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

import akka.util.ByteString
import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.tailrec

/**
  * An object that can be encoded as an element of an event stream.
  */
sealed trait EventStreamElement {

  /**
    * Encode this object as an element of an event stream.
    * @return UTF-8 encoded `akka.util.ByteString`
    */
  def encode: ByteString
}

object ServerSentEvent {

  /**
    * An empty [[ServerSentEvent]] that can be used as a heartbeat. Filtered out by clients.
    */
  object Heartbeat extends EventStreamElement {
    override def encode = ByteString("\n", UTF_8.name)
  }

  /**
    * A retry [[ServerSentEvent]] without any data associated.
    * @param retry the reconnection time in milliseconds.
    */
  case class Retry(retry: Int) extends EventStreamElement {
    require(retry > 0L, "Retry must be a positive number!")

    override def encode = ByteString(s"retry: $retry\n\n", UTF_8.name)
  }

  /**
    * An eventId which resets the last event ID to the empty string, meaning no `Last-Event-ID` header will be sent
    * in the event of a reconnection being attempted.
    */
  val emptyId: Option[String] = Some("")

  /**
    * A [[ServerSentEvent]] with the data set to the empty string that might be used as a heartbeat. Attention: not
    * filtered out by clients!
    */
  @deprecated("Use Heartbeat instead!", "2.8.0")
  val heartbeat: ServerSentEvent = new ServerSentEvent("")

  /**
    * Creates a [[ServerSentEvent]] with event type.
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    */
  def apply(data: String, eventType: String): ServerSentEvent =
    new ServerSentEvent(data, Some(eventType))

  /**
    * Creates a [[ServerSentEvent]] with event type and id.
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    * @param id event id, must not contain \n or \r
    */
  def apply(data: String, eventType: String, id: String): ServerSentEvent =
    new ServerSentEvent(data, Some(eventType), Some(id))

  /**
    * Creates a [[ServerSentEvent]] with event type, id and retry interval.
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    * @param id event id, must not contain \n or \r
    * @param retry the reconnection time in milliseconds.
    */
  def apply(data: String,
            eventType: String,
            id: String,
            retry: Int): ServerSentEvent =
    new ServerSentEvent(data, Some(eventType), Some(id), Some(retry))

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]] without event type.
    *
    * @param data data which may span multiple lines
    */
  def create(data: String): ServerSentEvent = new ServerSentEvent(data)

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]] with event type.
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    */
  def create(data: String, eventType: String): ServerSentEvent =
    new ServerSentEvent(data, Some(eventType))

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]] with event type and id.
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    * @param id event id, must not contain \n or \r
    */
  def create(data: String, eventType: String, id: String): ServerSentEvent =
    new ServerSentEvent(data, Some(eventType), Some(id))

  // Public domain algorithm: http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
  // We want powers of two both because they typically work better with the allocator,
  // and because we want to minimize reallocations/buffer growth.
  private def nextPowerOfTwoBiggerThan(n: Int) = {
    var m = n - 1
    m |= m >> 1
    m |= m >> 2
    m |= m >> 4
    m |= m >> 8
    m |= m >> 16
    m + 1
  }
}

/**
  * Representation of a Server-Sent Event.
  *
  * @param data data which may be empty or span multiple lines
  * @param eventType optional event type, must not contain \n or \r
  * @param id event id, must not contain \n or \r
  * @param retry the reconnection time in milliseconds.
  */
final case class ServerSentEvent(data: String,
                                 eventType: Option[String] = None,
                                 id: Option[String] = None,
                                 retry: Option[Int] = None)
    extends EventStreamElement {
  import ServerSentEvent._
  require(eventType.forall(_.forall(c => c != '\n' && c != '\r')),
          "Event type must not contain \\n or \\r!")
  require(id.forall(_.forall(c => c != '\n' && c != '\r')),
          "Id must not contain \\n or \\r!")
  require(retry.forall(_ > 0L), "Retry must be a positive number!")

  /**
    * Encodes this Server-Sent Event to an `akka.util.ByteString` according to the
    * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
    *
    * @return message converted to UTF-8 encoded `akka.util.ByteString`
    */
  override def encode = ByteString(toString, UTF_8.name)

  /**
    * Converts to an `akka.util.ByteString` according to the
    * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
    *
    * @return message converted to UTF-8 encoded `akka.util.ByteString`
    */
  @deprecated("Use encode instead!", "1.8.0")
  def toByteString: ByteString = encode

  /**
    * Converts to a `java.lang.String` according to the
    * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
    *
    * @return message converted to `java.lang.String`
    */
  override def toString = {
    // Why 8? "data:" == 5 + \n\n (1 data (at least) and 1 ending) == 2 and then we add 1 extra to allocate
    //        a bigger memory slab than data.length since we're going to add data ("data:" + "\n") per line
    // Why 7? "event:" + \n == 7 chars
    // Why 4? "id:" + \n == 4 chars
    // Why 17? "retry:" + \n + Integer.Max decimal places
    val builder = new StringBuilder(
      nextPowerOfTwoBiggerThan(
        8 + data.length + eventType.fold(0)(_.length + 7) + id.fold(0)(
          _.length + 4) + retry.fold(0)(_ => 17)
      )
    )
    @tailrec def appendData(s: String, index: Int = 0): Unit = {
      @tailrec def addLine(index: Int): Int =
        if (index >= s.length)
          -1
        else {
          val c = s.charAt(index)
          builder.append(c)
          if (c == '\n') index + 1 else addLine(index + 1)
        }
      builder.append("data: ")
      addLine(index) match {
        case -1 => builder.append('\n')
        case i  => appendData(s, i)
      }
    }
    appendData(data)
    if (eventType.isDefined)
      builder.append("event: ").append(eventType.get).append('\n')
    if (id.isDefined) builder.append("id: ").append(id.get).append('\n')
    if (retry.isDefined)
      builder.append("retry: ").append(retry.get).append('\n')
    builder.append('\n').toString
  }
}
