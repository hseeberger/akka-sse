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

import akka.http.scaladsl.model.{ HttpCharsets, MediaType }

/**
 * Media types for Server-Sent Events.
 */
object MediaTypes {

  /**
   * Media type for Server-Sent Events as required by the
   * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
   */
  val `text/event-stream`: MediaType = MediaType.custom(
    "text",
    "event-stream",
    MediaType.Encoding.Fixed(HttpCharsets.`UTF-8`)
  )
}
