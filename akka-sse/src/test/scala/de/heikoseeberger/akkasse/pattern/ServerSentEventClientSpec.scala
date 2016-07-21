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

import akka.Done
import akka.actor.ActorDSL.actor
import akka.actor.{ Actor, ActorLogging, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.pipe
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, ThrottleMode }
import akka.testkit.TestDuration
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object ServerSentEventClientSpec {

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
            case e: NumberFormatException =>
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
    val id = n.toString
    if (setEventId) ServerSentEvent(id, id = Some(id)) else ServerSentEvent(id)
  }
}

class ServerSentEventClientSpec extends BaseSpec {
  import ServerSentEventClientSpec._

  "ServerSentEventClient" should {
    "communicate correctly with an instable HTTP server" in {
      val host = "localhost"
      val port = 9999
      val server =
        actor(new Server(host, port, Server.route(setEventId = true)))
      val nrOfSamples = 20
      val handler = Sink.fold[Vector[ServerSentEvent], ServerSentEvent](
          Vector.empty
      )(_ :+ _)
      val events =
        ServerSentEventClient(Uri(s"http://$host:$port"), handler, Some("2"))
          .throttle(1, 500.milliseconds, 1, ThrottleMode.Shaping)
          .mapAsync(1)(identity)
          .mapConcat(identity)
          .take(nrOfSamples)
          .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      val expected = Vector
        .iterate(3, nrOfSamples)(_ + 1)
        .map(toServerSentEvent(setEventId = true))
      Await.result(events, 20.seconds.dilated) shouldBe expected
      system.stop(server)
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
        ServerSentEventClient(Uri(s"http://$host:$port"), handler, Some("2"))
          .mapAsync(1)(identity)
          .mapConcat(identity)
          .take(nrOfSamples)
          .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      val expexted = Vector
        .tabulate(20)(n => n % 2 + 3)
        .map(toServerSentEvent(setEventId = false))
      Await.result(events, 3.seconds.dilated) shouldBe expexted
      system.stop(server)
    }
  }
}
