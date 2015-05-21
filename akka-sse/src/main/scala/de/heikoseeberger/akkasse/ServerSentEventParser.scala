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

import akka.stream.stage.{ Context, PushStage }

private object ServerSentEventParser {

  private final val LF = "\n"

  private final val Data = "data"

  private final val Event = "event"

  private val linePattern = """([^:]+): ?(.*)""".r

  private def parseServerSentEvent(lines: Seq[String]) = {
    val valuesByField = lines
      .collect { case linePattern(field @ (Data | Event), value) => field -> value }
      .groupBy(_._1)
    val data = valuesByField.getOrElse(Data, Vector.empty).map(_._2).mkString(LF)
    val event = valuesByField.getOrElse(Event, Vector.empty).lastOption.map(_._2)
    ServerSentEvent(data, event)
  }
}

private final class ServerSentEventParser(maxSize: Int) extends PushStage[String, ServerSentEvent] {
  import ServerSentEventParser._

  private var lines = Vector.empty[String]

  override def onPush(line: String, ctx: Context[ServerSentEvent]) =
    if (line.nonEmpty) {
      lines :+= line
      if (lines.map(_.length).sum > maxSize)
        ctx.fail(new IllegalStateException(s"maxSize of $maxSize exceeded!"))
      else
        ctx.pull()
    } else {
      val event = parseServerSentEvent(lines)
      lines = Vector.empty
      ctx.push(event)
    }
}
