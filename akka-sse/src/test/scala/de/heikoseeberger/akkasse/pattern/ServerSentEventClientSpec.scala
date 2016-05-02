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
package pattern

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.testkit.TestDuration
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Random

class ServerSentEventClientSpec extends BaseSpec {
  import EventStreamMarshalling._

  private final val Host = "localhost"

  private final val Port = 9999

  private val server = { // Responds with SSE source of 10 elements starting from the given id + 1 or from 1
    def route = {
      import Directives._
      get {
        optionalHeaderValueByName(`Last-Event-ID`.name) { lastEventId =>
          try {
            val fromSeqNo = lastEventId.getOrElse("0").trim.toInt + 1
            complete {
              Source
                .fromIterator(() => Iterator.from(fromSeqNo).take(Random.nextInt(10)))
                .map(toServerSentEvent)
            }
          } catch {
            case e: NumberFormatException =>
              complete(HttpResponse(BadRequest, entity = HttpEntity(
                `text/event-stream`,
                "Integral number expected for Last-Event-ID header!".getBytes(UTF_8)
              )))
          }
        }
      }
    }
    Await.result(Http().bindAndHandle(route, Host, Port), 3.seconds.dilated)
  }

  "ServerSentEventClient" should {
    "handle the SSE connection correctly" in {
      val nrOfSamples = 100
      val connection = Flow[Option[String]].mapAsync(1) { lastEventId =>
        val fromSeqNo = lastEventId.getOrElse("0").trim.toInt + 1
        val events = Source
          .fromIterator(() => Iterator.from(fromSeqNo).take(Random.nextInt(10)))
          .map(toServerSentEvent)
        Marshal(events).to[HttpResponse]
      }
      val handler = Sink.fold[Vector[ServerSentEvent], ServerSentEvent](Vector.empty)(_ :+ _)
      val events = ServerSentEventClient(connection, handler)
        .mapAsync(1)(identity)
        .mapConcat(identity)
        .take(nrOfSamples)
        .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      Await.result(events, 3.seconds) shouldBe 1.to(nrOfSamples).map(toServerSentEvent)
    }

    "communicate with the HTTP server correctly" in {
      val nrOfSamples = 10
      val handler = Sink.fold[Vector[ServerSentEvent], ServerSentEvent](Vector.empty)(_ :+ _)
      val events = ServerSentEventClient(s"http://$Host:$Port", handler)
        .mapAsync(1)(identity)
        .mapConcat(identity)
        .take(nrOfSamples)
        .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      Await.result(events, 3.seconds) shouldBe 1.to(nrOfSamples).map(toServerSentEvent)
    }
  }

  override protected def afterAll() = {
    server.unbind()
    super.afterAll()
  }

  private def toServerSentEvent(n: Int) = {
    val id = n.toString
    ServerSentEvent(id, id = Some(id))
  }
}
