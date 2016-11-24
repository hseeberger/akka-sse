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

package de.heikoseeberger.akkasse.client

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
import de.heikoseeberger.akkasse.{ EventStream, ServerSentEvent }
import scala.concurrent.{ Future, Promise }

/**
  * This stream processing stage establishes a quasi-continuous [[EventStream]]
  * from the given URI.
  *
  * A single [[EventStream]] is obtained from the URI. Once completed, either
  * normally or by failure, a next one is obtained thereby sending a
  * Last-Evend-ID header if available. This continues in an endless cycle.
  *
  * The shape of this processing stage is a source of [[ServerSentEvent]]. To
  * take effect it must be run. Progress (including termination) is controlled
  * by the connected flow or sink, e.g. a retry delay can be implemented by
  * streaming the materialized values of the handler via a throttle.
  *
  *{{{
  * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
  *                                               +---------------------+
  * |                                             |       trigger       | |
  *                                               +----------o----------+
  * |                                                        |            |
  *                                            Option[String]|
  * |                                                        v            |
  *              Option[String]                   +----------o----------+
  * |            +------------------------------->o        merge        | |
  *              |                                +----------o----------+
  * |            |                                           |            |
  *              |                             Option[String]|
  * |            |                                           v            |
  *   +----------o----------+                     +----------o----------+
  * | | currentLastEventId  |                     |    eventStreams     | |
  *   +----------o----------+                     +----------o----------+
  * |            ^                                           |            |
  *              |           (EventSource, Future[LastEvent])|
  * |            |                                           v            |
  *              |                                +----------o----------+
  * |            +--------------------------------o        unzip        | |
  *              Future[LastEvent]                +----------o----------+
  * |                                                        |            |
  *                                               EventSource|
  * |                                                        v            |
  *                                               +----------o----------+
  * |                                  +----------o       flatten       | |
  *                     ServerSentEvent|          +---------------------+
  * |                                  v                                  |
  *  - - - - - - - - - - - - - - - - - o - - - - - - - - - - - - - - - - -
  *}}}
  */
object EventSource {

  private type LastEvent = Option[ServerSentEvent]

  private val noEvents = Future.successful(Source.empty[ServerSentEvent])

  /**
    * @param uri URI with absolute path, e.g. "http://myserver/events
    * @param send function to send a HTTP request
    * @param lastEventId initial value for Last-Evend-ID header, optional
    * @param mat implicit `Materializer`
    * @return source of materialized values of the handler
    */
  def apply(
      uri: Uri,
      send: HttpRequest => Future[HttpResponse],
      lastEventId: Option[String] = None
  )(implicit mat: Materializer): EventStream = {

    val eventStreams = {
      def getEventStream(lastEventId: Option[String]) = {
        import EventStreamUnmarshalling._
        import mat.executionContext
        val request = {
          val r = Get(uri).addHeader(Accept(`text/event-stream`))
          lastEventId.foldLeft(r)((r, i) => r.addHeader(`Last-Event-ID`(i)))
        }
        send(request)
          .flatMap(Unmarshal(_).to[EventStream])
          .fallbackTo(noEvents)
      }
      def enrichWithLastElement(eventStream: EventStream) = {
        val p = Promise[LastEvent]()
        val enriched =
          eventStream
            .alsoToMat(Sink.lastOption)(Keep.both)
            .mapMaterializedValue {
              case (m, f) =>
                p.completeWith(f)
                m
            }
        (enriched, p.future)
      }
      Flow[Option[String]]
        .mapAsync(1)(getEventStream)
        .map(enrichWithLastElement)
    }

    val currentLastEventId =
      Flow[Future[LastEvent]]
        .mapAsync(1)(identity)
        .scan(lastEventId)((prev, event) => event.flatMap(_.id).orElse(prev))
        .drop(1)

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val trigger = builder.add(Source.single(lastEventId))
      val merge   = builder.add(Merge[Option[String]](2))
      val unzip   = builder.add(Unzip[EventStream, Future[LastEvent]]())
      val flatten = builder.add(Flow[EventStream].flatMapConcat(identity))
      // format: OFF
      trigger ~> merge ~>    eventStreams    ~> unzip.in
                 merge <~ currentLastEventId <~ unzip.out1
                                                unzip.out0 ~> flatten
      // format: ON
      SourceShape(flatten.out)
    })
  }
}
