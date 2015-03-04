/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.nio.charset.{ Charset, StandardCharsets }
import scala.annotation.tailrec

object ServerSentEventParser {

  private final val LF = "\n"

  private final val Data = "data"

  private final val Event = "event"

  private val linePattern = """([^:]+): ?(.*)""".r

  def apply(maxSize: Int = 1048576, charset: Charset = StandardCharsets.UTF_8) =
    new ServerSentEventParser(maxSize, charset)

  private def parseServerSentEvent(content: String) = {
    val valuesByField = content
      .split(LF)
      .toVector
      .collect { case linePattern(field, value) if field == Data || field == Event => field -> value }
      .groupBy(_._1)
    val data = valuesByField.getOrElse(Data, Vector.empty).map(_._2).mkString(LF)
    val event = valuesByField.getOrElse(Event, Vector.empty).lastOption.map(_._2)
    if (data.isEmpty) None else Some(ServerSentEvent(data, event))
  }
}

final class ServerSentEventParser private (maxSize: Int, charset: Charset)
    extends StatefulStage[ByteString, ServerSentEvent] {

  import ServerSentEventParser._

  private val separator = ByteString("\n\n", charset.name)

  private val firstSeparatorByte = separator.head

  private var buffer = ByteString.empty

  private var nextPossibleMatch = 0

  override def initial = new State {

    override def onPush(bytes: ByteString, ctx: Context[ServerSentEvent]) = {
      buffer ++= bytes
      if (buffer.size > maxSize)
        ctx.fail(new IllegalStateException(s"maxSize of $maxSize exceeded!"))
      else
        emit(parse().iterator, ctx)
    }

    @tailrec
    private def parse(events: Vector[ServerSentEvent] = Vector.empty): Vector[ServerSentEvent] = {
      val possibleMatch = buffer.indexOf(firstSeparatorByte, nextPossibleMatch)
      if (possibleMatch == -1) {
        nextPossibleMatch = buffer.size
        events
      } else {
        val size = possibleMatch + separator.size
        if (size > buffer.size) {
          nextPossibleMatch = possibleMatch
          events
        } else if (buffer.slice(possibleMatch, size) == separator) {
          val content = buffer.slice(0, size).utf8String
          buffer = buffer.drop(size)
          nextPossibleMatch -= size
          parseServerSentEvent(content) match {
            case Some(event) => parse(events :+ event)
            case None        => parse(events)
          }
        } else {
          nextPossibleMatch += 1
          parse(events)
        }
      }
    }
  }
}
