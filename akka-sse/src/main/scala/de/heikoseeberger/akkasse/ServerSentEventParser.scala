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
import scala.util.control.NonFatal

private object ServerSentEventParser {

  private final val LF = "\n"

  private final val Data = "data"

  private final val Event = "event"

  private final val Id = "id"

  private final val Retry = "retry"

  private val linePattern = """([^:]+): ?(.*)""".r

  private def parseFields(lines: Vector[String]) = {
    var data = Vector.empty[String]
    var eventType = null: String
    var id = null: String
    var retry = null: String
    for (line <- lines) {
      line match {
        case Data  => data :+= ""
        case Event => eventType = ""
        case Id    => id = ""
        case Retry => retry = ""
        case linePattern(field @ (Data | Event | Id | Retry), value) => field match {
          case Data  => data :+= value
          case Event => eventType = value
          case Id    => id = value
          case Retry => retry = value
          case _     =>
        }
        case _ =>
      }
    }
    (data, eventType, id, retry)
  }

  private def silentlyToInt(s: String) = try Some(s.trim.toInt) catch { case NonFatal(_) => None }
}

private final class ServerSentEventParser(maxEventSize: Int) extends GraphStage[FlowShape[String, ServerSentEvent]] {

  override val shape = FlowShape(Inlet[String]("ServerSentEventParser.in"), Outlet[ServerSentEvent]("ServerSentEventParser.out"))

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    import ServerSentEventParser._
    import shape._

    setHandler(in, new InHandler {
      private var lines = Vector.empty[String]
      private var linesSize = 0L
      override def onPush() = {
        val (newLines, newSize) = grab(in) match {
          case "" => // A server-sent event is terminated with a new line, i.e. an empty line
            val (data, eventType, id, retry) = parseFields(lines)
            if (data.isEmpty)
              pull(in)
            else
              push(out, ServerSentEvent(
                data.mkString(LF),
                Option(eventType),
                Option(id),
                Option(retry).flatMap(silentlyToInt)
              ))
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
