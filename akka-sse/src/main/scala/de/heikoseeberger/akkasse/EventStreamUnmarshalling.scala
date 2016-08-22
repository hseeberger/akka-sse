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

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`

/**
  * Importing [[EventStreamUnmarshalling.feu]] lets a `HttpEntity` with a `text/event-stream`
  * media type be unmarshallable to a `Source[ServerSentEvent, Any]`.
  */
object EventStreamUnmarshalling extends EventStreamUnmarshalling

/**
  * Mixing in this trait lets a `HttpEntity` with a `text/event-stream` media type be unmarshallable to a
  * `Source[ServerSentEvent, Any]`.
  */
trait EventStreamUnmarshalling {

  private val _maxEventSize = maxEventSize

  private val _maxLineSize = maxLineSize

  /**
    * The maximum size of a server-sent event for the event Stream parser; 8KiB by default.
    */
  protected def maxEventSize: Int = 8192

  /**
    * The maximum size of a line for the event Stream parser; 8KiB by default.
    */
  protected def maxLineSize: Int = 8192

  implicit final def feu: FromEntityUnmarshaller[Source[ServerSentEvent, Any]] = {
    def events(entity: HttpEntity) =
      entity.dataBytes.via(
          EventStreamParser(maxLineSize = _maxLineSize,
                            maxEventSize = _maxEventSize)
      )
    Unmarshaller.strict(events).forContentTypes(`text/event-stream`)
  }
}
