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

object ServerSentEvent {

  /**
    * An empty [[ServerSentEvent]] which can be used as a heartbeat.
    */
  val heartbeat: ServerSentEvent =
    ServerSentEvent()

  /**
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    */
  def apply(data: String): ServerSentEvent =
    new ServerSentEvent(Some(data))

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    */
  def create(data: String): ServerSentEvent =
    new ServerSentEvent(Some(data))

  /**
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    */
  def apply(data: String, eventType: String): ServerSentEvent =
    new ServerSentEvent(Some(data), Some(eventType))

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    */
  def create(data: String, eventType: String): ServerSentEvent =
    new ServerSentEvent(Some(data), Some(eventType))

  /**
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    * @param id event id, must not contain \n or \r
    */
  def apply(data: String, eventType: String, id: String): ServerSentEvent =
    new ServerSentEvent(Some(data), Some(eventType), Some(id))

  /**
    * Java API.
    *
    * Creates a [[ServerSentEvent]].
    *
    * @param data data which may span multiple lines
    * @param eventType event type, must not contain \n or \r
    * @param id event id, must not contain \n or \r
    */
  def create(data: String, eventType: String, id: String): ServerSentEvent =
    new ServerSentEvent(Some(data), Some(eventType), Some(id))

  private def noNewLine(s: String) = s.forall(c => c != '\n' && c != '\r')

  // Public domain algorithm: http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
  // We want powers of two both because they typically work better with the
  // allocator, and because we want to minimize reallocations/buffer growth.
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
  * @param retry the reconnection time in milliseconds
  */
final case class ServerSentEvent(data: Option[String] = None,
                                 eventType: Option[String] = None,
                                 id: Option[String] = None,
                                 retry: Option[Int] = None) {
  import ServerSentEvent._

  require(eventType.forall(noNewLine),
          "eventType must not contain \\n or \\r!")
  require(id.forall(noNewLine), "id must not contain \\n or \\r!")
  require(retry.forall(_ > 0), "retry must be a positive number!")

  /**
    * Encodes this Server-Sent Event to an `akka.util.ByteString` according to the
    * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
    *
    * @return message converted to UTF-8 encoded `akka.util.ByteString`
    */
  def encode: ByteString =
    ByteString(toString, UTF_8.name)

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
        8 +
          data.map(_.length).getOrElse(0) +
          eventType.fold(0)(_.length + 7) +
          id.fold(0)(_.length + 4) + retry.fold(0)(_ => 17)
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
    if (data.isDefined)
      appendData(data.get)
    if (eventType.isDefined)
      builder.append("event: ").append(eventType.get).append('\n')
    if (id.isDefined)
      builder.append("id: ").append(id.get).append('\n')
    if (retry.isDefined)
      builder.append("retry: ").append(retry.get).append('\n')
    builder.append('\n').toString
  }
}
