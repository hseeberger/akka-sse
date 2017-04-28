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

  final class Builder {

    private var data      = Vector.empty[String]
    private var eventType = null: String
    private var id        = null: String
    private var retry     = null: String

    private var _size = 0

    def appendData(value: String): Unit = {
      _size += 5 + value.length
      data :+= value
    }

    def setEventType(value: String): Unit = {
      val oldSize = if (eventType == null) 0 else 6 + eventType.length
      _size += 6 + value.length - oldSize
      eventType = value
    }

    def setId(value: String): Unit = {
      val oldSize = if (id == null) 0 else 3 + id.length
      _size += 3 + value.length - oldSize
      id = value
    }

    def setRetry(value: String): Unit = {
      val oldSize = if (retry == null) 0 else 6 + retry.length
      _size += 6 + value.length - oldSize
      retry = value
    }

    def size: Int =
      _size

    def build(): ServerSentEvent =
      ServerSentEvent(
        if (data.nonEmpty) Some(data.mkString("\n")) else None,
        Option(eventType),
        Option(id),
        try { if (retry ne null) Some(retry.trim.toInt) else None } catch {
          case _: NumberFormatException => None
        }
      )

    def reset(): Unit = {
      data = Vector.empty[String]
      eventType = null
      id = null
      retry = null
      _size = 0
    }
  }

  private final val Data  = "data"
  private final val Event = "event"
  private final val Id    = "id"
  private final val Retry = "retry"

  //TODO switch to a regex which only matches the fields above
  private val linePattern = """([^:]+): ?(.*)""".r
}

private final class ServerSentEventParser(maxEventSize: Int)
    extends GraphStage[FlowShape[String, ServerSentEvent]] {

  override val shape = FlowShape(
    Inlet[String]("ServerSentEventParser.in"),
    Outlet[ServerSentEvent]("ServerSentEventParser.out")
  )

  override def createLogic(attributes: Attributes) =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      import ServerSentEventParser._
      import shape._

      private val builder = new Builder()

      setHandlers(in, out, this)

      override def onPush() = {
        val line = grab(in)
        if (line == "") { // An event is terminated with a new line
          push(out, builder.build())
          builder.reset()
        } else if (builder.size + line.length <= maxEventSize) {
          line match {
            case Data  => builder.appendData("")
            case Event => builder.setEventType("")
            case Id    => builder.setId("")
            case Retry => builder.setRetry("")
            case linePattern(field, value) =>
              field match {
                case Data            => builder.appendData(value)
                case Event           => builder.setEventType(value)
                case Id              => builder.setId(value)
                case Retry           => builder.setRetry(value)
                case badFormatString => // TODO: Consider if these should be reported!
              }
            case badFormatString => // TODO: Consider if these should be reported!
          }
          pull(in)
        } else {
          failStage(new IllegalStateException(s"maxEventSize of $maxEventSize exceeded!"))
          builder.reset()
        }
      }

      override def onPull() = pull(in)
    }
}
