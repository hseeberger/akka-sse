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

import akka.stream.stage.{ Context, StatefulStage }
import akka.util.ByteString

private object LineParser {

  final val CR = '\r'.toByte

  final val LF = '\n'.toByte
}

private final class LineParser(maxLineSize: Int) extends StatefulStage[ByteString, String] {
  import LineParser._

  private var buffer = ByteString.empty

  override def initial = new State {
    override def onPush(bytes: ByteString, ctx: Context[String]) = {
      buffer ++= bytes

      val parsedLines = lines().iterator

      if (buffer.size > maxLineSize)
        ctx.fail(new IllegalStateException(s"maxSize of $maxLineSize exceeded!"))
      else
        emit(parsedLines, ctx)
    }

    private def lines(): Vector[String] = {
      val (lines, nrOfConsumedBytes, _) = (buffer :+ 0)
        .zipWithIndex
        .sliding(2)
        .collect {
          case Seq((CR, n), (LF, _)) => (n, 2)
          case Seq((CR, n), _)       => (n, 1)
          case Seq((LF, n), _)       => (n, 1)
        }
        .foldLeft((Vector.empty[String], 0, false)) {
          case ((slices, from, false), (until, k)) => (slices :+ buffer.slice(from, until).utf8String, until + k, k == 2)
          case ((slices, _, _), (until, _))        => (slices, until + 1, false)
        }
      buffer = buffer.drop(nrOfConsumedBytes)
      lines
    }
  }
}
