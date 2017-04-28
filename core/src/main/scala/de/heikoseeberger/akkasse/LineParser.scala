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

  private val cr = '\r'.toByte

  private val lf = '\n'.toByte
}

private final class LineParser(maxLineSize: Int)
    extends GraphStage[FlowShape[ByteString, String]] {

  override val shape =
    FlowShape(Inlet[ByteString]("LineParser.in"), Outlet[String]("LineParser.out"))

  override def createLogic(attributes: Attributes) =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      import LineParser._
      import shape._

      private var buffer = ByteString.empty

      setHandlers(in, out, this)

      override def onPush() = {
        @tailrec
        def parseLines(bs: ByteString,
                       from: Int = 0,
                       at: Int = 0,
                       parsedLines: Vector[String] = Vector.empty): (ByteString, Vector[String]) =
          if (at >= bs.length || (at == bs.length - 1 && bs(at) == cr))
            (bs.drop(from), parsedLines)
          else
            bs(at) match {
              // Lookahead for LF after CR
              case `cr` if bs(at + 1) == lf =>
                parseLines(bs, at + 2, at + 2, parsedLines :+ bs.slice(from, at).utf8String)
              // a CR or LF means we found a new slice
              case `cr` | `lf` =>
                parseLines(bs, at + 1, at + 1, parsedLines :+ bs.slice(from, at).utf8String)
              // for other input, simply advance
              case _ =>
                parseLines(bs, from, at + 1, parsedLines)
            }
        buffer = parseLines(buffer ++ grab(in)) match {
          case (remaining, _) if remaining.size > maxLineSize =>
            failStage(new IllegalStateException(s"maxLineSize of $maxLineSize exceeded!"))
            ByteString.empty // Clear buffer
          case (remaining, parsedLines) =>
            if (parsedLines.nonEmpty)
              emitMultiple(out, parsedLines)
            else
              pull(in)
            remaining
        }
      }

      override def onPull() = pull(in)
    }
}
