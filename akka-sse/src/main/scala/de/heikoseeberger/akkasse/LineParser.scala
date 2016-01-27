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
import scala.annotation.tailrec

private object LineParser {

  final val CR = '\r'.toByte

  final val LF = '\n'.toByte
}

private final class LineParser(maxLineSize: Int) extends GraphStage[FlowShape[ByteString, String]] {
  override val shape = FlowShape(Inlet[ByteString]("LineParser.in"), Outlet[String]("LineParser.out"))

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    import LineParser._
    import shape._

    setHandler(in, new InHandler {
      private var buffer = ByteString.empty
      override def onPush() = {
        @tailrec
        def parseLines(
          buffer: ByteString,
          from: Int = 0,
          at: Int = 0,
          parsedLines: Vector[String] = Vector.empty
        ): (ByteString, Vector[String]) =
          if (at >= buffer.length) (buffer.drop(from), parsedLines)
          else buffer(at) match {
            case CR if buffer(math.min(at + 1, buffer.length - 1)) == LF => // Lookahead for LF after CR
              parseLines(buffer, at + 2, at + 2, parsedLines :+ buffer.slice(from, at).utf8String)
            case CR | LF => // a CR or LF means we found a new slice
              parseLines(buffer, at + 1, at + 1, parsedLines :+ buffer.slice(from, at).utf8String)
            case _ => // for other input, simply advance
              parseLines(buffer, from, at + 1, parsedLines)
          }
        buffer = parseLines(buffer ++ grab(in)) match {
          case (remaining, _) if remaining.size > maxLineSize =>
            failStage(new IllegalStateException(s"maxLineSize of $maxLineSize exceeded!"))
            ByteString.empty // Clear buffer
          case (remaining, parsedLines) =>
            emitMultiple(out, parsedLines)
            remaining
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }
}
