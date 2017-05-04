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
package scaladsl
package model
package headers

import akka.http.scaladsl.model.headers.{ ModeledCustomHeader, ModeledCustomHeaderCompanion }
import scala.util.Try

object `Last-Event-ID` extends ModeledCustomHeaderCompanion[`Last-Event-ID`] {

  override val name = "Last-Event-ID"

  override def parse(value: String) = Try(new `Last-Event-ID`(value))
}

/**
  * The Last-Event-ID header is sent by a client to the server to signal the ID of the last sever-sent event received.
  *
  * @param value value of the last event ID, encoded as UTF-8 string
  */
final case class `Last-Event-ID`(value: String)
    extends ModeledCustomHeader[`Last-Event-ID`]
    with de.heikoseeberger.akkasse.javadsl.model.headers.LastEventId {

  override val companion = `Last-Event-ID`

  override val renderInRequests = true

  override val renderInResponses = false

  override def getValue = value
}
