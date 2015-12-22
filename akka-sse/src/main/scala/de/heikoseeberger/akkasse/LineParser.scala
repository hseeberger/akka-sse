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

import akka.stream.{ Attributes, Outlet, FlowShape, Inlet }
import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.util.ByteString

private object LineParser {

  final val CR = '\r'.toByte

  final val LF = '\n'.toByte
}

private final class LineParser(maxLineSize: Int) extends GraphStage[FlowShape[ByteString, String]] {
  import LineParser._

  private var buffer = ByteString.empty

  private val in = Inlet[ByteString]("LineParser.in")
  private val out = Outlet[String]("LineParser.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          buffer ++= grab(in)
          val parsedLines = lines()
          if (buffer.size > maxLineSize)
            failStage(new IllegalStateException(s"maxLineSize of $maxLineSize exceeded!"))
          else
            emitMultiple(out, parsedLines)
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit =
          pull(in)
      })

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
