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
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent, WithHeartbeats }
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object TimeServer {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()
    import system.dispatcher
    Http().bindAndHandle(route(system), "127.0.0.1", 9000)
  }

  def route(system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
    import Directives._
    import EventStreamMarshalling._
    get {
      complete {
        Source.tick(2.seconds, 2.seconds, ())
          .map(_ => LocalTime.now())
          .map(dateTimeToServerSentEvent)
          .via(WithHeartbeats(1.second))
      }
    }
  }

  def dateTimeToServerSentEvent(time: LocalTime): ServerSentEvent = ServerSentEvent(
    DateTimeFormatter.ISO_LOCAL_TIME.format(time)
  )
}
