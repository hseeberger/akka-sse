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

package de.heikoseeberger.akkasse.example

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Directives
import akka.stream.{ ActorFlowMaterializer, FlowMaterializer }
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{ Duration, DurationInt }

object TimeServer extends Directives with EventStreamMarshalling {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorFlowMaterializer()
    import system.dispatcher
    Http().bindAndHandle(route, "127.0.0.1", 9000)
  }

  private implicit def dateTimeToServerSentEvent(dateTime: LocalTime): ServerSentEvent =
    ServerSentEvent(DateTimeFormatter.ISO_LOCAL_TIME.format(dateTime))

  private def route(implicit ec: ExecutionContext, mat: FlowMaterializer) =
    get {
      complete(Source(Duration.Zero, 2 seconds, None).map(_ => LocalTime.now()))
    }
}
