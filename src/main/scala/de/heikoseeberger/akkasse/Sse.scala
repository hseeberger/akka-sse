/*
 * Copyright 2014 Heiko Seeberger
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

import akka.http.model.{ ContentType, HttpCharsets, MediaType }
import akka.util.ByteString
import scala.annotation.tailrec

/**
 * Defines an SSE message and a content type for SSE.
 */
object Sse {

  /**
   * Reprsentation of an SSE message.
   * @param data data which may span multiple lines
   * @param event optional event type, must not contain \n or \r
   */
  final case class Message(data: String, event: Option[String] = None) {
    require(event.forall(_.forall(c => c != '\n' && c != '\r')), "Event must not contain \\n or \\r!")

    /**
     * Convert to a `java.lang.String`
     * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
     * @return message converted to `java.lang.String`
     */
    override def toString: String = {
      @tailrec def addLines(builder: StringBuilder, label: String, s: String, index: Int): StringBuilder = {
        @tailrec def addLine(index: Int): Int =
          if (index >= s.length)
            -1
          else {
            val c = s.charAt(index)
            builder.append(c)
            if (c == '\n')
              index + 1
            else
              addLine(index + 1)
          }

        builder.append(label)
        addLine(index) match {
          case -1    => builder.append('\n')
          case index => addLines(builder, label, s, index)
        }
      }

      def addData(builder: StringBuilder): StringBuilder =
        addLines(builder, "data:", data, 0).append('\n')

      def addEvent(builder: StringBuilder): StringBuilder =
        event match {
          case Some(e) => addLines(builder, "event:", e, 0)
          case None    => builder
        }

      def newBuilder(): StringBuilder = {
        // Public domain algorithm: http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
        // We want powers of two both because they typically work better with the allocator,
        // and because we want to minimize reallocations/buffer growth.
        def nextPowerOfTwoBiggerThan(i: Int): Int = {
          var v = i
          v -= 1
          v |= v >> 1
          v |= v >> 2
          v |= v >> 4
          v |= v >> 8
          v |= v >> 16
          v + 1
        }
        // Why 8? "data:" == 5 + \n\n (1 data (at least) and 1 ending) == 2 and then we add 1 extra to allocate
        //        a bigger memory slab than data.length since we're going to add data ("data:" + "\n") per line
        // Why 7? "event:" + \n == 7 chars
        new StringBuilder(nextPowerOfTwoBiggerThan(8 + data.length + event.fold(0)(_.length + 7)))
      }

      addData(addEvent(newBuilder())).toString()
    }

    /**
     * Convert to an `akka.util.ByteString`
     * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
     * @return message converted to UTF-8 encoded `akka.util.ByteString`
     */
    def toByteString: ByteString =
      ByteString(toString, "UTF-8")
  }

  /**
   * SSE content type as required by the
   * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
   */
  val `text/event-stream`: ContentType =
    ContentType(MediaType.custom("text", "event-stream"), HttpCharsets.`UTF-8`)
}
