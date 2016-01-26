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
import scala.util.Try

private object ServerSentEventParser {

  private final val LF = "\n"

  private final val Data = "data"

  private final val Event = "event"

  private final val Id = "id"

  private final val Retry = "retry"

  private val LinePattern = """([^:]+): ?(.*)""".r

  private val emptyLineMap = Map.empty[String, Vector[String]].withDefault(_ => Vector.empty)

  private def parseServerSentEvent(lines: Vector[String]): ServerSentEvent = {
    val values = lines
      .foldLeft(emptyLineMap) {
        case (m, "")                                                      => m
        case (m, LinePattern(field @ (Data | Event | Id | Retry), value)) => m.updated(field, m(field) :+ value)
        case (m, field)                                                   => m.updated(field, m(field) :+ "")
      }
    ServerSentEvent(
      data = values(Data).mkString(LF),
      eventType = values(Event).lastOption,
      id = values(Id).lastOption,
      retry = values(Retry).lastOption.flatMap { s => Try(s.trim.toInt).toOption }
    )
  }
}

private final class ServerSentEventParser(maxEventSize: Int) extends GraphStage[FlowShape[String, ServerSentEvent]] {
  import ServerSentEventParser._

  private val in = Inlet[String]("server-sent-event-parser.in")

  private val out = Outlet[ServerSentEvent]("server-sent-event-parser.out")

  override val shape = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    private var lines = Vector.empty[String]
    private var linesSize = 0L

    setHandler(in, new InHandler {
      override def onPush() = {
        val (newLines, newSize) =
          grab(in) match {
            case "" => // A server-sent event is terminated with a new line, i.e. an empty line
              emit(out, parseServerSentEvent(lines))
              (Vector.empty, 0L)
            case line if linesSize + line.length > maxEventSize =>
              failStage(new IllegalStateException(s"maxEventSize of $maxEventSize exceeded!"))
              (Vector.empty, 0L)
            case line =>
              pull(in)
              (lines :+ line, linesSize + line.length)
          }
        lines = newLines
        linesSize = newSize
      }
    })

    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }
}
