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
package marshalling

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.RouteTest
import akka.http.scaladsl.testkit.TestFrameworkInterface.Scalatest
import akka.stream.scaladsl.{ Sink, Source }
import de.heikoseeberger.akkasse.scaladsl.model.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent
import org.scalatest.{ Matchers, WordSpec }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

final class EventStreamMarshallingSpec extends WordSpec with Matchers with RouteTest with Scalatest {
  import Directives._
  import EventStreamMarshalling._

  "A source of ServerSentEvents" should {
    "be marshallable to a HTTP response" in {
      val events = 1.to(666).map(n => ServerSentEvent(n.toString))
      val route  = complete(Source(events))
      Get() ~> route ~> check {
        mediaType shouldBe `text/event-stream`
        Await.result(responseEntity.dataBytes.runWith(Sink.seq), 3.seconds) shouldBe events.map(_.encode)
      }
    }
  }
}
