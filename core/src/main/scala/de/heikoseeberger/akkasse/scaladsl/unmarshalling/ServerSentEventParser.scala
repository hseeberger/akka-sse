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
package scaladsl
package unmarshalling

import akka.stream.stage.{ GraphStage, GraphStageLogic, InHandler, OutHandler }
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent

private object ServerSentEventParser {

  final object PosInt {
    def unapply(s: String): Option[Int] =
      try { Some(s.trim.toInt) } catch { case _: NumberFormatException => None }
  }

  final class Builder {

    private var data = Vector.empty[String]

    private var `type` = Option.empty[String]

    private var id = Option.empty[String]

    private var retry = Option.empty[Int]

    private var _size = 0

    def appendData(value: String): Unit = {
      _size += 5 + value.length
      data :+= value
    }

    def setType(value: String): Unit = {
      val oldSize = `type`.fold(0)(6 + _.length)
      _size += 6 + value.length - oldSize
      `type` = Some(value)
    }

    def setId(value: String): Unit = {
      val oldSize = id.fold(0)(3 + _.length)
      _size += 3 + value.length - oldSize
      id = Some(value)
    }

    def setRetry(value: Int, length: Int): Unit = {
      val oldSize = retry.fold(0)(6 + _.toString.length)
      _size += 6 + length - oldSize
      retry = Some(value)
    }

    def hasData: Boolean =
      data.nonEmpty

    def size: Int =
      _size

    def build(): ServerSentEvent =
      ServerSentEvent(data.mkString("\n"), `type`, id, retry)

    def reset(): Unit = {
      data = Vector.empty[String]
      `type` = None
      id = None
      retry = None
      _size = 0
    }
  }

  private final val Data = "data"

  private final val Event = "event"

  private final val Id = "id"

  private final val Retry = "retry"

  private val field = """([^:]+): ?(.*)""".r
}

private final class ServerSentEventParser(maxEventSize: Int) extends GraphStage[FlowShape[String, ServerSentEvent]] {

  override val shape =
    FlowShape(Inlet[String]("ServerSentEventParser.in"), Outlet[ServerSentEvent]("ServerSentEventParser.out"))

  override def createLogic(attributes: Attributes) =
    new GraphStageLogic(shape) with InHandler with OutHandler {
      import ServerSentEventParser._
      import shape._

      private val builder = new Builder()

      setHandlers(in, out, this)

      override def onPush() = {
        val line = grab(in)
        if (line == "") { // An event is terminated with a new line
          if (builder.hasData) // Events without data are ignored according to the spec
            push(out, builder.build())
          else
            pull(in)
          builder.reset()
        } else if (builder.size + line.length <= maxEventSize) {
          line match {
            case Id                                 => builder.setId("")
            case field(Data, data) if data.nonEmpty => builder.appendData(data)
            case field(Event, t) if t.nonEmpty      => builder.setType(t)
            case field(Id, id)                      => builder.setId(id)
            case field(Retry, s @ PosInt(retry))    => builder.setRetry(retry, s.length)
            case _                                  => // ignore according to spec
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
