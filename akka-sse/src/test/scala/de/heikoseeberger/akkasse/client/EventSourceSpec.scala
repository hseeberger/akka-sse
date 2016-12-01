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
import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{ HttpEntity, HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.pattern.pipe
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.{ ActorMaterializer, ThrottleMode }
import akka.testkit.SocketUtil
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.duration.DurationInt

object EventSourceSpec {

  object Server {

    private case object Bind
    private case object Unbind

    private def route(size: Int, setEventId: Boolean): Route = {
      import Directives._
      import EventStreamMarshalling._
      get {
        optionalHeaderValueByName(`Last-Event-ID`.name) { lastEventId =>
          try {
            val fromSeqNo = lastEventId.map(_.trim.toInt).getOrElse(0) + 1
            complete {
              Source(fromSeqNo.until(fromSeqNo + size))
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

  class Server(address: String,
               port: Int,
               size: Int,
               shouldSetEventId: Boolean = false)
      extends Actor
      with ActorLogging {
    import Server._
    import context.dispatcher

    private implicit val mat = ActorMaterializer()

    self ! Bind

    override def receive = unbound

    private def unbound: Receive = {
      case Bind =>
        Http(context.system)
          .bindAndHandle(route(size, shouldSetEventId), address, port)
          .pipeTo(self)
        context.become(binding)
    }

    private def binding: Receive = {
      case serverBinding @ Http.ServerBinding(socketAddress) =>
        log.info("Listening on {}", socketAddress)
        context.system.scheduler.scheduleOnce(1500.millis, self, Unbind)
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
        context.system.scheduler.scheduleOnce(500.millis, self, Bind)
        context.become(unbound)

      case Status.Failure(cause) =>
        log.error(cause, s"Can't unbind from {}!", socketAddress)
        context.stop(self)
    }
  }

  private def toServerSentEvent(setEventId: Boolean)(n: Int) = {
    val data  = Some(n.toString)
    val event = ServerSentEvent(data)
    if (setEventId) event.copy(id = data) else event
  }

  private def hostAndPort() = {
    val address = SocketUtil.temporaryServerAddress()
    (address.getAddress.getHostAddress, address.getPort)
  }
}

class EventSourceSpec extends BaseSpec {
  import EventSourceSpec._

  "EventSource" should {
    "communicate correctly with an instable HTTP server" in {
      val nrOfSamples  = 20
      val (host, port) = hostAndPort()
      val server       = system.actorOf(Props(new Server(host, port, 2, true)))
      val events =
        EventSource(Uri(s"http://$host:$port"), send, Some("2"))
          .throttle(1, 500.milliseconds, 1, ThrottleMode.Shaping) // Make sure unbinding happens!
          .take(nrOfSamples)
          .runWith(Sink.seq)
      val expected =
        Seq
          .tabulate(nrOfSamples)(_ + 3)
          .map(toServerSentEvent(setEventId = true))
      events.map(_ shouldBe expected).andThen { case _ => system.stop(server) }
    }

    "apply the initial last event ID if the server doesn't set the event ID" in {
      val nrOfSamples  = 20
      val (host, port) = hostAndPort()
      val server       = system.actorOf(Props(new Server(host, port, 2)))
      val events =
        EventSource(Uri(s"http://$host:$port"), send, Some("2"))
          .take(nrOfSamples)
          .runWith(Sink.seq)
      val expected =
        Seq
          .tabulate(nrOfSamples)(_ % 2 + 3)
          .map(toServerSentEvent(setEventId = false))
      events.map(_ shouldBe expected).andThen { case _ => system.stop(server) }
    }
  }

  private def send(request: HttpRequest) = Http().singleRequest(request)
}
