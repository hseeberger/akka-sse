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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpResponse, Uri }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Keep, Merge, Sink, Source, Unzip }
import akka.stream.{ Materializer, SinkShape, SourceShape }
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import de.heikoseeberger.commons.akka.stream.ScanAhead
import scala.concurrent.{ ExecutionContext, Future }

object ServerSentEventClient {

  type Handler[A] = Sink[ServerSentEvent, A]

  /**
   * Creates a continuous flow of [[ServerSentEvent]]s from the given URI and streams it into the given handler. Once a
   * source of [[ServerSentEvent]]s obtained via the connection is completed, a next one is obtained thereby sending the
   * Last-Evend-ID header if there is a last event id.
   *
   * @param uri URI with absolute path, e.g. "http://myserver/events
   * @param handler handler for [[ServerSentEvent]]s
   * @param ec implicit `ExecutionContext`
   * @param mat implicit `Materializer`
   * @param system implicit [[ActorSystem]]
   * @return source of materialized values of the handler
   */
  def apply[A](uri: String, handler: Handler[A])(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem): Source[A, Any] =
    apply(Uri(uri), handler)

  /**
   * Creates a continuous flow of [[ServerSentEvent]]s from the given URI and streams it into the given handler. Once a
   * source of [[ServerSentEvent]]s obtained via the connection is completed, a next one is obtained thereby sending the
   * Last-Evend-ID header if there is a last event id.
   *
   * @param uri URI with absolute path, e.g. "http://myserver/events
   * @param handler handler for [[ServerSentEvent]]s
   * @param ec implicit `ExecutionContext`
   * @param mat implicit `Materializer`
   * @param system implicit [[ActorSystem]]
   * @return source of materialized values of the handler
   */
  def apply[A](uri: Uri, handler: Handler[A])(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem): Source[A, Any] = {
    val eventStreamRequest = Get(uri = uri).addHeader(Accept(`text/event-stream`))
    val connection = Flow[Option[String]]
      .map(_.foldLeft(eventStreamRequest)((request, lastEventId) => request.addHeader(`Last-Event-ID`(lastEventId))))
      .mapAsync(1)(request => Http().singleRequest(request))
    apply(connection, handler)
  }

  /**
   * Creates a continuous flow of [[ServerSentEvent]]s from the given connection and streams it into the given handler.
   * Once a source of [[ServerSentEvent]]s obtained via the connection is completed, a next one is obtained thereby
   * passing the last event id if any.
   *
   * @param connection HTTP connection
   * @param handler handler for [[ServerSentEvent]]s
   * @param ec implicit `ExecutionContext`
   * @param mat implicit `Materializer`
   * @return source of materialized values of the handler
   */
  def apply[A](connection: Flow[Option[String], HttpResponse, Any], handler: Handler[A])(implicit ec: ExecutionContext, mat: Materializer): Source[A, Any] = {
    import EventStreamUnmarshalling._
    import GraphDSL.Implicits._

    val runSource = {
      val handlerAndLastEventId = {
        val lastEventId = Sink.fold[Option[String], ServerSentEvent](None)((acc, event) => event.id.orElse(acc))
        Sink.fromGraph(GraphDSL.create(handler, lastEventId)(Keep.both) { implicit builder => (handler, lastEventId) =>
          val bcast = builder.add(Broadcast[ServerSentEvent](2, eagerCancel = true))
          // format: OFF
          bcast ~> handler
          bcast ~> lastEventId
          // format: ON
          SinkShape(bcast.in)
        })
      }
      connection
        .mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, Any]])
        .map(_.runWith(handlerAndLastEventId))
    }

    Source.fromGraph(GraphDSL.create(runSource) { implicit builder => runSource =>
      val trigger = Source.single(None)
      val merge = builder.add(Merge[Option[String]](2))
      val unzip = builder.add(Unzip[A, Future[Option[String]]]())
      val lastEventId = Flow[Future[Option[String]]]
        .mapAsync(1)(identity)
        .via(ScanAhead(Option.empty[String])((acc, id) => id.orElse(acc)))
      // format: OFF
      trigger ~> merge ~> runSource   ~> unzip.in
                 merge <~ lastEventId <~ unzip.out1
      // format: ON
      SourceShape(unzip.out0)
    })
  }
}
