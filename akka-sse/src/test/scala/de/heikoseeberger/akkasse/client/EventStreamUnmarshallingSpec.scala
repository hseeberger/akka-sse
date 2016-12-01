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
package client

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{ Sink, Source }
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`

class EventStreamUnmarshallingSpec extends BaseSpec {
  import EventStreamUnmarshalling._

  "A HTTP entity with media-type text/event-stream" should {
    "be unmarshallable to an EventStream" in {
      val events = 1.to(666).map(intToServerSentEvent)
      val data   = Source(events).map(_.encode)
      val entity = HttpEntity(`text/event-stream`, data)
      Unmarshal(entity)
        .to[EventStream]
        .flatMap(_.runWith(Sink.seq))
        .map(_ shouldBe events)
    }
  }

  private def intToServerSentEvent(n: Int) = ServerSentEvent(Some(n.toString))
}
