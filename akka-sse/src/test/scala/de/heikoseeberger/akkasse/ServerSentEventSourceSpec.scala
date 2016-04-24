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

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.{ Flow, Source }
import akka.stream.testkit.scaladsl.TestSink

class ServerSentEventSourceSpec extends BaseSpec {
  import EventStreamMarshalling._

  "ServerSentEventSource" should {
    "concatenate the sources correctly" in {
      def toServerSentEvent(n: Int) = {
        val id = n.toString
        ServerSentEvent(id, id = Some(id))
      }
      var isEmpty = false
      val sources = Flow[String]
        .statefulMapConcat { () => s =>
          val n = s.toInt
          isEmpty = !isEmpty
          val ss =
            if (isEmpty)
              Vector(Source.empty[ServerSentEvent])
            else
              Vector(Source(n.until(n + 3).map(toServerSentEvent)))
          ss.map(s => Marshal(s).to[HttpResponse])
        }
        .mapAsync(1)(identity)
      ServerSentEventSource(sources)
        .map(_.data.toInt)
        .runWith(TestSink.probe)
        .request(9)
        .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9)
    }
  }
}
