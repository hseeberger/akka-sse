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

import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import akka.util.ByteString

private object LineParser {

  final val CR = '\r'.toByte

  final val LF = '\n'.toByte
}

private final class LineParser(maxLineSize: Int) extends GraphStage[FlowShape[ByteString, String]] {
  import LineParser._

  override val shape = FlowShape(Inlet[ByteString]("in"), Outlet[String]("out"))

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    import shape._

    private var buffer = ByteString.empty

    setHandler(in, new InHandler {
      override def onPush() = {
        def parseLines(buffer: ByteString) = {
          val (lines, nrOfConsumedBytes, _) = (buffer :+ 0) // The trailing 0 makes sure sliding windows have size 2 for all bytes
            .iterator
            .zipWithIndex
            .sliding(2)
            .collect { // Collect the delimiters with their (start) position
              case Seq((CR, n), (LF, _)) => (n, 2)
              case Seq((CR, n), _)       => (n, 1)
              case Seq((LF, n), _)       => (n, 1)
            }
            .foldLeft((Vector.empty[String], 0, 1)) { // The 3rd value is for knowing if the last delimiter was \r\n in which case the next (\r) must be ignored
              case ((slices, from, 1), (until, k)) => (slices :+ buffer.slice(from, until).utf8String, until + k, k)
              case ((slices, from, _), _)          => (slices, from, 1)
            }
          (buffer.drop(nrOfConsumedBytes), lines)
        }

        buffer = parseLines(buffer ++ grab(in)) match {
          case (buf, _) if buf.size > maxLineSize =>
            failStage(new IllegalStateException(s"maxLineSize of $maxLineSize exceeded!"))
            ByteString.empty // Clear buffer
          case (buf, parsedLines) =>
            emitMultiple(out, parsedLines)
            buf
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }
}
