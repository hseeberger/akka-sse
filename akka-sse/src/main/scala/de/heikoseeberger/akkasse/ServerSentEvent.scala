/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkasse

import akka.util.ByteString
import scala.annotation.tailrec

object ServerSentEvent {

  /**
   * Creates a [[ServerSentEvent]] with event type.
   * @param data data which may span multiple lines
   * @param event event type, must not contain \n or \r
   */
  def apply(data: String, event: String) = new ServerSentEvent(data, Some(event))

  /**
   * Java API.
   * Creates a [[ServerSentEvent]] without event type.
   * @param data data which may span multiple lines
   */
  def create(data: String) = new ServerSentEvent(data)

  /**
   * Java API.
   * Creates a [[ServerSentEvent]] with event type.
   * @param data data which may span multiple lines
   * @param event event type, must not contain \n or \r
   */
  def create(data: String, event: String) = new ServerSentEvent(data, Some(event))

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

  /**
   * A comment event used for sending heartbeats to the client.
   */
  final val Heartbeat = ServerSentEvent("heartbeat", comment = true)
}

/**
 * Representation of a Server-Sent Event.
 * @param data data which may span multiple lines
 * @param eventType optional event type, must not contain \n or \r
 * @param comment determines if this event is a comment, i.e. has no field name
 */
final case class ServerSentEvent private[akkasse] (data: String, eventType: Option[String] = None, comment: Boolean = false) {

  import ServerSentEvent._

  require(eventType.forall(_.forall(c => c != '\n' && c != '\r')), "Event must not contain \\n or \\r!")

  /**
   * Convert to a `java.lang.String`
   * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
   * @return message converted to `java.lang.String`
   */
  override def toString = {
    @tailrec def addLines(builder: StringBuilder, label: String, s: String, index: Int): StringBuilder = {
      @tailrec def addLine(index: Int): Int =
        if (index >= s.length)
          -1
        else {
          val c = s.charAt(index)
          builder.append(c)
          if (c == '\n') index + 1 else addLine(index + 1)
        }
      builder.append(label)
      addLine(index) match {
        case -1    => builder.append('\n')
        case index => addLines(builder, label, s, index)
      }
    }
    def addData(builder: StringBuilder) =
      addLines(builder, (if (comment) "" else "data") + ":", data, 0).append('\n')
    def addEvent(builder: StringBuilder) = eventType match {
      case Some(e) => addLines(builder, "event:", e, 0)
      case None    => builder
    }
    // Why 8? "data:" == 5 + \n\n (1 data (at least) and 1 ending) == 2 and then we add 1 extra to allocate
    //        a bigger memory slab than data.length since we're going to add data ("data:" + "\n") per line
    // Why 7? "event:" + \n == 7 chars
    val builder = new StringBuilder(nextPowerOfTwoBiggerThan(8 + data.length + eventType.fold(0)(_.length + 7)))
    addData(addEvent(builder)).toString()
  }

  /**
   * Convert to an `akka.util.ByteString`
   * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
   * @return message converted to UTF-8 encoded `akka.util.ByteString`
   */
  def toByteString = ByteString(toString, "UTF-8")
}
