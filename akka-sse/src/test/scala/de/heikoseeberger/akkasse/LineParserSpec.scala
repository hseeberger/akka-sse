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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class LineParserSpec extends BaseSpec {

  "A LineParser" should {

    "parse lines terminated with either CR, LF or CRLF" in {
      val input = "line1\nline2\rline3\r\n\n"
      val lines = Source.single(ByteString(input))
        .via(new LineParser(1048576))
        .runFold(Vector.empty[String])(_ :+ _)
      Await.result(lines, 1 second) shouldBe Vector("line1", "line2", "line3", "")
    }

    "ignore a trailing non-terminated line" in {
      val input = "line1\nline2\rline3\r\n\nincomplete"
      val lines = Source.single(ByteString(input))
        .via(new LineParser(1048576))
        .runFold(Vector.empty[String])(_ :+ _)
      Await.result(lines, 1 second) shouldBe Vector("line1", "line2", "line3", "")
    }
  }
}
