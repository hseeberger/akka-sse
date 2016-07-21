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

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.util.ByteString

object EventStreamParser {

  /**
    * Flow that converts raw byte string input into [[ServerSentEvent]]s.
    * This API is made for use in non akka-http clients, like Play's WSClient.
    *
    * @param maxLineSize The maximum size of a line for the event Stream parser; 8KiB by default.
    * @param maxEventSize The maximum size of a server-sent event for the event Stream parser; 8KiB by default.
    */
  def apply(
      maxLineSize: Int = 8192,
      maxEventSize: Int = 8192
  ): Flow[ByteString, ServerSentEvent, NotUsed] =
    Flow[ByteString]
      .via(new LineParser(maxLineSize))
      .via(new ServerSentEventParser(maxEventSize))
}
