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

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ Matchers, WordSpec }

class ServerSentEventSpec
    extends WordSpec
    with Matchers
    with GeneratorDrivenPropertyChecks {

  "Creating a ServerSentEvent" should {
    "throw an IllegalArgumentException if the event type contains a \n or \r character" in {
      an[IllegalArgumentException] should be thrownBy ServerSentEvent(
          "data",
          "event-type\n"
      )
      an[IllegalArgumentException] should be thrownBy ServerSentEvent(
          "data",
          "event-type\revent-type"
      )
      an[IllegalArgumentException] should be thrownBy ServerSentEvent(
          "data",
          id = Some("id\n")
      )
      an[IllegalArgumentException] should be thrownBy ServerSentEvent(
          "data",
          retry = Some(-1)
      )
    }
  }

  "Calling toString" should {
    "return a single data line for single line message" in {
      val event = ServerSentEvent("line")
      event.toString shouldBe "data:line\n\n"
    }

    "return multiple data lines for a multi-line message" in {
      val event = ServerSentEvent("line1\nline2\n")
      event.toString shouldBe "data:line1\ndata:line2\ndata:\n\n"
    }

    "return multiple data lines and an event line for a multi-line message with a defined event type" in {
      val event = ServerSentEvent("line1\nline2", "event-type")
      event.toString shouldBe "data:line1\ndata:line2\nevent:event-type\n\n"
    }

    "return a single id field after the data fields" in {
      val event = ServerSentEvent("line1", eventType = None, id = Some("1"))
      event.toString shouldBe "data:line1\nid:1\n\n"
    }

    "return a single retry field after the data fields" in {
      val event =
        ServerSentEvent("line1", eventType = None, id = None, retry = Some(42))
      event.toString shouldBe "data:line1\nretry:42\n\n"
    }
  }

  "Calling toByteString" should {
    "return a correctly converted ByteString" in {
      forAll { (data: String) =>
        val event = ServerSentEvent(data)
        event.encode.utf8String shouldBe event.toString
      }
    }
  }
}
