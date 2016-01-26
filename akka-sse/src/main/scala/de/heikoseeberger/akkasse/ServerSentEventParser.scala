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

private object ServerSentEventParser {

  private final val LF = "\n"

  private final val Data = "data"

  private final val Event = "event"

  private final val Id = "id"

  private final val Retry = "retry"

  private val linePattern = """([^:]+): ?(.*)""".r

  private def parseServerSentEvent(lines: Seq[String]) = {
    val valuesByField = lines
      .collect {
        case linePattern(field @ (Data | Event | Id | Retry), value) => field -> value
        case field if field.nonEmpty                                 => field -> ""
      }
      .groupBy(_._1)
    def values(field: String) = valuesByField
      .getOrElse(field, Vector.empty)
      .map(_._2)
    val data = values(Data).mkString(LF)
    val event = values(Event).lastOption
    val idField = values(Id).lastOption
    val retry = values(Retry)
      .lastOption
      .flatMap { s =>
        try
          Some(s.trim.toInt)
        catch {
          case _: NumberFormatException => None
        }
      }
    ServerSentEvent(data, event, idField, retry)
  }
}

private final class ServerSentEventParser(maxEventSize: Int) extends GraphStage[FlowShape[String, ServerSentEvent]] {
  import ServerSentEventParser._

  private val in = Inlet[String]("server-sent-event-parser.in")

  private val out = Outlet[ServerSentEvent]("server-sent-event-parser.out")

  override val shape = FlowShape(in, out)

  private var lines = Vector.empty[String]

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush() = {
        val line = grab(in)
        if (line.nonEmpty) { // A server-sent event is terminated with a new line, i.e. an empty line
          lines :+= line
          if (lines.map(_.length).sum > maxEventSize)
            failStage(new IllegalStateException(s"maxEventSize of $maxEventSize exceeded!"))
          else
            pull(in)
        } else {
          val event = parseServerSentEvent(lines)
          lines = Vector.empty
          emit(out, event)
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }

  //  override def onPush(line: String, ctx: Context[ServerSentEvent]) =
  //    if (line.nonEmpty) {
  //      lines :+= line
  //      if (lines.map(_.length).sum > maxEventSize)
  //        ctx.fail(new IllegalStateException(s"maxEventSize of $maxEventSize exceeded!"))
  //      else
  //        ctx.pull()
  //    } else {
  //      val event = parseServerSentEvent(lines)
  //      lines = Vector.empty
  //      ctx.push(event)
  //    }
}
