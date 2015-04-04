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
import akka.http.client.RequestBuilding.Get
import akka.http.unmarshalling.Unmarshal
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.{ EventStreamUnmarshalling, ServerSentEvent }
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object TimeClient extends EventStreamUnmarshalling {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorFlowMaterializer()
    import system.dispatcher

    Source.single(Get())
      .via(Http().outgoingConnection("127.0.0.1", 9000))
      .mapAsync(xxx => Unmarshal(xxx).to[Source[ServerSentEvent, Unit]])
      .runForeach(_.map(serverSentEventToDateTime).runForeach(println))
  }

  private def serverSentEventToDateTime(event: ServerSentEvent): LocalTime =
    LocalTime.parse(event.data, DateTimeFormatter.ISO_LOCAL_TIME)
}
