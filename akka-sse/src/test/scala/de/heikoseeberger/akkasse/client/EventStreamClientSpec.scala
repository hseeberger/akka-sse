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
package client

import akka.Done
import akka.actor.ActorDSL.actor
import akka.actor.{ Actor, ActorLogging, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.pipe
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, ThrottleMode }
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.duration.DurationInt

object EventStreamClientSpec {

  object Server {

    private case object Bind
    private case object Unbind

    def route(setEventId: Boolean) = {
      import Directives._
      import EventStreamMarshalling._
      get {
        optionalHeaderValueByName(`Last-Event-ID`.name) { lastEventId =>
          try {
            val fromSeqNo = lastEventId.getOrElse("0").trim.toInt + 1
            complete {
              Source
                .fromIterator(() => Iterator.from(fromSeqNo).take(2))
                .map(toServerSentEvent(setEventId))
            }
          } catch {
            case _: NumberFormatException =>
              complete(
                HttpResponse(
                  BadRequest,
                  entity = HttpEntity(
                    `text/event-stream`,
                    "Integral number expected for Last-Event-ID header!"
                      .getBytes(UTF_8)
                  )
                )
              )
          }
        }
      }
    }
  }

  class Server(address: String, port: Int, route: Route)
      extends Actor
      with ActorLogging {
    import Server._
    import context.dispatcher

    private implicit val mat = ActorMaterializer()

    self ! Bind

    override def receive = unbound

    private def unbound: Receive = {
      case Bind =>
        Http(context.system).bindAndHandle(route, address, port).pipeTo(self)
        context.become(binding)
    }

    private def binding: Receive = {
      case serverBinding @ Http.ServerBinding(socketAddress) =>
        log.info("Listening on {}", socketAddress)
        context.system.scheduler.scheduleOnce(1500.milliseconds, self, Unbind)
        context.become(bound(serverBinding))

      case Status.Failure(cause) =>
        log.error(cause, s"Can't bind to {}:{}!", address, port)
        context.stop(self)
    }

    private def bound(serverBinding: Http.ServerBinding): Receive = {
      case Unbind =>
        serverBinding.unbind().map(_ => Done).pipeTo(self)
        context.become(unbinding(serverBinding.localAddress))
    }

    private def unbinding(socketAddress: InetSocketAddress): Receive = {
      case Done =>
        log.info("Stopped listening on {}", socketAddress)
        context.system.scheduler.scheduleOnce(500.milliseconds, self, Bind)
        context.become(unbound)

      case Status.Failure(cause) =>
        log.error(cause, s"Can't unbind from {}!", socketAddress)
        context.stop(self)
    }
  }

  private def toServerSentEvent(setEventId: Boolean)(n: Int) = {
    val id    = n.toString
    val event = ServerSentEvent(Some(id))
    if (setEventId) event.copy(id = Some(id)) else event
  }
}

class EventStreamClientSpec extends BaseSpec {
  import EventStreamClientSpec._

  "ServerSentEventClient" should {
    "communicate correctly with an instable HTTP server" in {
      val host = "localhost"
      val port = 9999
      val server =
        actor(new Server(host, port, Server.route(setEventId = true)))
      val nrOfSamples = 20
      val handler     = Sink.seq[ServerSentEvent]
      val events =
        EventStreamClient(Uri(s"http://$host:$port"), handler, send, Some("2"))
          .throttle(1, 500.milliseconds, 1, ThrottleMode.Shaping)
          .mapAsync(1)(identity)
          .mapConcat(identity)
          .take(nrOfSamples)
          .runWith(Sink.seq)
      val expected =
        Seq
          .iterate(3, nrOfSamples)(_ + 1)
          .map(toServerSentEvent(setEventId = true))
      events
        .map(_ shouldBe expected)
        .andThen({ case _ => system.stop(server) })
    }

    "apply the initial last event ID if the server doesn't set the event ID" in {
      val host = "localhost"
      val port = 9998
      val server =
        actor(new Server(host, port, Server.route(setEventId = false)))
      val nrOfSamples = 20
      val handler = Sink.fold[Vector[ServerSentEvent], ServerSentEvent](
        Vector.empty
      )(_ :+ _)
      val events =
        EventStreamClient(Uri(s"http://$host:$port"), handler, send, Some("2"))
          .mapAsync(1)(identity)
          .mapConcat(identity)
          .take(nrOfSamples)
          .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      val expected =
        Vector
          .tabulate(20)(n => n % 2 + 3)
          .map(toServerSentEvent(setEventId = false))
      events.onComplete(_ => system.stop(server))
      events.map(_ shouldBe expected)
    }
  }

  private def send(request: HttpRequest) = Http().singleRequest(request)
}
