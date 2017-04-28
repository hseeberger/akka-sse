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

import akka.{ Done, NotUsed }
import akka.actor.{ Actor, ActorLogging, Props, Status }
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.http.scaladsl.server.{ Directives, Route }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import akka.testkit.SocketUtil
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import java.nio.charset.StandardCharsets.UTF_8
import org.scalatest.{ AsyncWordSpec, Matchers }

object EventStreamUnmarshallingSpec {

  final object Server {

    private def route(size: Int): Route = {
      import Directives._
      import EventStreamMarshalling._

      def toServerSentEvent(n: Int) = ServerSentEvent(n.toString)

      get {
        optionalHeaderValueByName(`Last-Event-ID`.name) { lastEventId =>
          try {
            val fromSeqNo = lastEventId.map(_.trim.toInt).getOrElse(0) + 1
            complete {
              Source(fromSeqNo.until(fromSeqNo + size)).map(toServerSentEvent)
            }
          } catch {
            case _: NumberFormatException =>
              complete(
                HttpResponse(
                  BadRequest,
                  entity = HttpEntity(
                    `text/event-stream`,
                    "Integral number expected for Last-Event-ID header!".getBytes(UTF_8)
                  )
                )
              )
          }
        }
      }
    }
  }

  final class Server(address: String, port: Int, size: Int) extends Actor with ActorLogging {
    import Server._
    import context.dispatcher

    private implicit val mat = ActorMaterializer()

    Http(context.system).bindAndHandle(route(size), address, port).pipeTo(self)

    override def receive = {
      case Http.ServerBinding(socketAddress) =>
        log.info("Listening on {}", socketAddress)
        context.become(Actor.emptyBehavior)

      case Status.Failure(cause) =>
        log.error(cause, s"Can't bind to {}:{}!", address, port)
        context.stop(self)
    }
  }

  private def hostAndPort() = {
    val address = SocketUtil.temporaryServerAddress()
    (address.getAddress.getHostAddress, address.getPort)
  }
}

final class EventStreamUnmarshallingSpec extends AsyncWordSpec with Matchers with AkkaSpec {
  import EventStreamUnmarshalling._
  import EventStreamUnmarshallingSpec._
  import RequestBuilding._

  "A HTTP entity with media-type text/event-stream" should {
    "be unmarshallable to an EventStream" in {
      val events = 1.to(666).map(n => ServerSentEvent(n.toString))
      val data   = Source(events).map(_.encode)
      val entity = HttpEntity(`text/event-stream`, data)
      Unmarshal(entity)
        .to[Source[ServerSentEvent, NotUsed]]
        .flatMap(_.runWith(Sink.seq))
        .map(_ shouldBe events)
    }

    "not be limited" in {
      val nrOfSamples  = 1024 // See application.conf
      val (host, port) = hostAndPort()
      val server       = system.actorOf(Props(new Server(host, port, nrOfSamples)))
      Http()
        .singleRequest(Get(s"http://$host:$port"))
        .flatMap {
          Unmarshal(_)
            .to[Source[ServerSentEvent, NotUsed]]
            .flatMap(_.take(nrOfSamples).runWith(Sink.ignore))
        }
        .map(_ shouldBe Done)
        .andThen { case _ => system.stop(server) }
    }
  }
}
