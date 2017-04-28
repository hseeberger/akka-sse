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

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalatest.{AsyncWordSpec, Matchers}

final class ServerSentEventParserSpec extends AsyncWordSpec with Matchers with AkkaSpec {

  "A ServerSentEventParser" should {
    "parse ServerSentEvents correctly" in {
      val input = """|data: message 1 line 1
                     |data:message 1 line 2
                     |
                     |data: message 2
                     |:This is a comment and must be ignored
                     |event: Only the last event should be considered
                     |event: message 2 event
                     |id: Only the last id should be considered
                     |id: 42
                     |retry: 512
                     |
                     |
                     |
                     |event: message 4 event
                     |id:
                     |
                     |data
                     |id
                     |
                     |retry: 512
                     |
                     |data: incomplete message
                     |""".stripMargin
      Source(input.split(f"%n").toVector)
        .via(new ServerSentEventParser(1048576))
        .runWith(Sink.seq)
        .map(
          _ shouldBe Vector(
            ServerSentEvent(Some("message 1 line 1\nmessage 1 line 2")),
            ServerSentEvent(Some("message 2"), Some("message 2 event"), Some("42"), Some(512)),
            ServerSentEvent(None, Some("message 4 event"), Some("")),
            ServerSentEvent(Some(""), None, Some("")),
            ServerSentEvent(retry = Some(512))
          )
        )
    }

    "ignore unparsable retry fields" in {
      val input = """|data: stuff
                     |retry: ten
                     |""".stripMargin
      Source(input.split(f"%n", -1).toVector)
        .via(new ServerSentEventParser(1048576))
        .runWith(Sink.seq)
        .map(_ shouldBe Vector(ServerSentEvent(Some("stuff"), retry = None)))
    }

    "work for issue 36" in {
      val input = "data: stuff\r\ndata: more\r\ndata: extra\n\n"
      Source
        .single(ByteString(input))
        .via(new LineParser(1048576))
        .via(new ServerSentEventParser(1048576))
        .runWith(Sink.seq)
        .map(_ shouldBe Vector(ServerSentEvent(Some("stuff\nmore\nextra"))))
    }
  }
}
