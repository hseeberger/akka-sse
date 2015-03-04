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

package de.heikoseeberger.akkasse.example

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.server.Directives
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object TimeServer {

  import Directives._
  import EventStreamMarshalling._

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    import system.dispatcher
    implicit val mat = ActorFlowMaterializer()

    // TODO Once https://github.com/akka/akka/issues/16972 is fixed, simplify this with `bindAndHandle` or such!
    Http()
      .bind("127.0.0.1", 9000)
      .to(Sink.foreach(_.flow.join(route).run()))
      .run()
  }

  private implicit def dateTimeToServerSentEvent(dateTime: LocalTime): ServerSentEvent =
    ServerSentEvent(DateTimeFormatter.ISO_LOCAL_TIME.format(dateTime))

  private def route(implicit ec: ExecutionContext, mat: ActorFlowMaterializer) =
    path("") {
      get {
        complete(Source(250 millis, 1 second, None).map(_ => LocalTime.now()))
      }
    }
}
