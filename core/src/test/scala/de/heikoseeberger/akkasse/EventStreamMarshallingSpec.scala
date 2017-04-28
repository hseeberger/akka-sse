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

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.{ Sink, Source }
import org.scalatest.{ AsyncWordSpec, Matchers }

final class EventStreamMarshallingSpec extends AsyncWordSpec with Matchers with AkkaSpec {
  import EventStreamMarshalling._

  "A source of ServerSentEvents" should {
    "be marshallable to a HTTP response" in {
      val events       = 1.to(666).map(intToServerSentEvent)
      val marshallable = Source(events): ToResponseMarshallable
      val response =
        marshallable(HttpRequest()).flatMap(_.entity.dataBytes.runWith(Sink.seq))
      response.map(_ shouldBe events.map(_.encode))
    }
  }

  private def intToServerSentEvent(n: Int) = ServerSentEvent(Some(n.toString))
}
