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

import akka.NotUsed
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal

import akka.stream.scaladsl.{
  Flow,
  GraphDSL,
  Keep,
  Merge,
  Sink,
  Source,
  Unzip
}
import akka.stream.{ Materializer, SourceShape }
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import scala.concurrent.{ ExecutionContext, Future }

object EventStreamClient {

  private val noEvents = Future.successful(Source.empty[ServerSentEvent])

  /**
    * This stream processing stage establishes and handles a quasi-continuous
    * [[EventStream]] from the given URI.
    *
    * A single [[EventStream]] is obtained from the URI and run with the
    * given handler. Once completed, either normally or by failure, a next one
    * is obtained thereby sending a Last-Evend-ID header if available. Hence
    * obtaining and handling happens in an endless cycle.
    *
    * The shape of this processing stage is a source of materialized values of
    * the given handler. To take effect it must be run. Progress (including
    * termination) is controlled by the connected sink, e.g. a retry delay can
    * be implemented by streaming the materialized values of the handler into a
    * throttle.
    *
    *{{{
    * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
    *                                               +---------------------+
    * |                                             |       trigger       |                                                             |
    *                                               +----------o----------+
    * |                                                        |                                                                        |
    *                                            Option[String]|
    * |                                                        v                                                                        |
    *              Option[String]                   +----------o----------+
    * |            +------------------------------->o        merge        |                                                             |
    *              |                                +----------o----------+
    * |            |                                           |                                                                        |
    *              |                             Option[String]|
    * |            |                                           v                  + - - - - - - - - - - - - - - - - - - - - - - - - - + |
    *   +----------o----------+                     +----------o----------+         +--------------+
    * | | currentLastEventId  |                     |      getEvents      |       | |    events    |                                  | |
    *   +----------o----------+                     +----------o----------+         +-------o------+
    * |            ^                                           |                  |         |                                         | |
    *              |               Source[ServerSentEvent, Any]|                            | ServerSentEvent
    * |            |                                           v                  |         |                                         | |
    *              |                                +----------o----------+  run            +------------------------+
    * |            |                                |       handle        |-------|         |                        |                | |
    *              |                                +----------o----------+                 |                        |
    * |            |                                           |                  |         v                        v                | |
    *              |       (Future[Option[ServerSentEvent]], A)|                    +-------o------+         +-------o------+
    * |            |                                           v                  | |   handler    |         |  lastOption  |         | |
    *              |                                +----------o----------+         +-------x------+         +-------x------+
    * |            +--------------------------------o        unzip        |       |         A         Future[Option[ServerSentEvent]] | |
    *              Future[Option[ServerSentEvent]]  +----------o----------+        - - - - - - - - - - - - -x- - - - - - - - - - - - -
    * |                                                        |                           (A, Future[Option[ServerSentEvent]])         |
    *                                                        A |
    * |                                                        v                                                                        |
    *  - - - - - - - - - - - - - - - - - - - - - - - - - - - - o - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
    *}}}
    *
    * @param uri URI with absolute path, e.g. "http://myserver/events
    * @param handler handler for [[ServerSentEvent]]s
    * @param lastEventId initial value for Last-Evend-ID header, optional
    * @param ec implicit `ExecutionContext`
    * @param mat implicit `Materializer`
    * @return source of materialized values of the handler
    */
  def apply[A](
      uri: Uri,
      handler: Sink[ServerSentEvent, A],
      send: HttpRequest => Future[HttpResponse],
      lastEventId: Option[String] = None
  )(implicit ec: ExecutionContext, mat: Materializer): Source[A, NotUsed] = {

    def getAndHandleEvents = {
      def getAndHandle(lastEventId: Option[String])
        : (Future[Option[ServerSentEvent]], A) = {
        import EventStreamUnmarshalling._
        val request = {
          val r = Get(uri).addHeader(Accept(`text/event-stream`))
          lastEventId.foldLeft(r)((r, i) => r.addHeader(`Last-Event-ID`(i)))
        }
        val events =
          send(request)
            .flatMap(Unmarshal(_).to[EventStream])
            .fallbackTo(noEvents)
        Source
          .fromFuture(events)
          .flatMapConcat(identity)
          .alsoToMat(Sink.lastOption)(Keep.right)
          .toMat(handler)(Keep.both)
          .run()
      }
      Flow[Option[String]].map(getAndHandle)
    }

    def currentLastEventId =
      Flow[Future[Option[ServerSentEvent]]]
        .mapAsync(1)(identity)
        .scan(lastEventId)((prev, event) => event.flatMap(_.id).orElse(prev))
        .drop(1)

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val trigger = builder.add(Source.single(lastEventId))
      val merge   = builder.add(Merge[Option[String]](2))
      val unzip   = builder.add(Unzip[Future[Option[ServerSentEvent]], A]())
      // format: OFF
      trigger ~> merge ~> getAndHandleEvents ~> unzip.in
                 merge <~ currentLastEventId <~ unzip.out0
      // format: ON
      SourceShape(unzip.out1)
    })
  }
}
