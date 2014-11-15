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

object Sse {

  /**
   * Reprsentation of a SSE message.
   * @param data data which may span multiple lines
   * @param event optional event type, must not contain \n or \r
   */
  case class Message(data: String, event: Option[String] = None) {
    require(event.fold(true)(e => eventPattern.matcher(e).matches()), "Event must not contain \n or \r!")

    /**
     * Convert to a `java.lang.String`
     * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
     * @return converted message
     */
    override def toString: String = {
      val dataString = data
        .split("\n", -1)
        .map(line => s"data:$line")
        .mkString("", "\n", "\n")
      event.foldLeft(s"$dataString\n")((d, e) => s"event:$e\n$d")
    }

    /**
     * Convert to an `akka.util.ByteString`
     * according to the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
     * @return converted message
     */
    def toByteString: ByteString =
      ByteString(toString)
  }

  /**
   * SSE Content type as required by the [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
   */
  val `text/event-stream`: ContentType =
    ContentType(MediaType.custom("text", "event-stream"), HttpCharsets.`UTF-8`)

  private val eventPattern = """[^\n\r]*""".r.pattern
}
