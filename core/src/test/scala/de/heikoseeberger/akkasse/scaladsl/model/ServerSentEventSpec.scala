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

import akka.util.ByteString
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ Matchers, WordSpec }

final class ServerSentEventSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks {

  "Creating a ServerSentEvent" should {
    "throw an IllegalArgumentException if type contains a \n or \r character" in {
      an[IllegalArgumentException] should be thrownBy ServerSentEvent("data", "type\n")
      an[IllegalArgumentException] should be thrownBy ServerSentEvent("data", "type\rtype")
    }

    "throw an IllegalArgumentException if id contains a \n or \r character" in {
      an[IllegalArgumentException] should be thrownBy ServerSentEvent("data", id = Some("id\n"))
      an[IllegalArgumentException] should be thrownBy ServerSentEvent("data", id = Some("id\rid"))
    }

    "throw an IllegalArgumentException if retry is not a positive number" in {
      forAll("retry") { (n: Int) =>
        whenever(n <= 0) {
          an[IllegalArgumentException] should be thrownBy ServerSentEvent("data", n)
        }
      }
    }
  }

  "Calling encode" should {
    "return a single data line" in {
      val event = ServerSentEvent(" ")
      event.encode shouldBe ByteString.fromString("data: \n\n")
    }

    "return multiple data lines" in {
      val event = ServerSentEvent("data1\ndata2\n")
      event.encode shouldBe ByteString.fromString("data:data1\ndata:data2\ndata:\n\n")
    }

    "return data lines and an type line" in {
      val event = ServerSentEvent("data1\ndata2", "type")
      event.encode shouldBe ByteString.fromString("data:data1\ndata:data2\nevent:type\n\n")
    }

    "return a data line and an id line" in {
      val event = ServerSentEvent("data", id = Some("id"))
      event.encode shouldBe ByteString.fromString("data:data\nid:id\n\n")
    }

    "return a retry line" in {
      val event = ServerSentEvent("data", 42)
      event.encode shouldBe ByteString.fromString("data:data\nretry:42\n\n")
    }

    "return all possible lines" in {
      val event = ServerSentEvent("data", Some("type"), Some("id"), Some(42))
      event.encode shouldBe ByteString.fromString("data:data\nevent:type\nid:id\nretry:42\n\n")
    }
  }
}
