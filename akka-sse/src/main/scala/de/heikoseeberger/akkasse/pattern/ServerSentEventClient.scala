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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, Uri }
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.contrib.{ Accumulate, LastElement }
import akka.stream.scaladsl.{ Flow, GraphDSL, Keep, Merge, Sink, Source, Unzip }
import akka.stream.{ DelayOverflowStrategy, Materializer, SourceShape }
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import scala.concurrent.duration.{ Duration, FiniteDuration }
import scala.concurrent.{ ExecutionContext, Future }

object ServerSentEventClient {

  /**
    * Creates a continuous source of [[ServerSentEvent]]s from the given URI and streams it into the given handler. Once a
    * source of [[ServerSentEvent]]s obtained via the connection is completed, a next one is obtained thereby sending the
    * Last-Evend-ID header if there is a last event id.
    *
    * @param uri URI with absolute path, e.g. "http://myserver/events
    * @param handler handler for [[ServerSentEvent]]s
    * @param lastEventId initial value for Last-Evend-ID header, optional
    * @param retryDelay delay before obtaining the next source from the URI
    * @param ec implicit `ExecutionContext`
    * @param mat implicit `Materializer`
    * @return source of materialized values of the handler
    */
  def apply[A](
      uri: Uri,
      handler: Sink[ServerSentEvent, A],
      send: HttpRequest => Future[HttpResponse],
      lastEventId: Option[String] = None,
      retryDelay: FiniteDuration = Duration.Zero
  )(implicit ec: ExecutionContext, mat: Materializer): Source[A, NotUsed] = {
    // Get the events, run them with the handler and return the last event and the mat value of the handler
    def getAndHandle(lastEventId: Option[String]) = {
      def getEvents = {
        import EventStreamUnmarshalling._
        val request = {
          val r = Get(uri).addHeader(Accept(`text/event-stream`))
          lastEventId.foldLeft(r) { (r, id) =>
            r.addHeader(`Last-Event-ID`(id))
          }
        }
        send(request).flatMap(Unmarshal(_).to[Source[ServerSentEvent, Any]])
      }
      Source
        .fromFuture(getEvents)
        .flatMapConcat(identity)
        .viaMat(LastElement())(Keep.right)
        .toMat(handler)(Keep.both)
        .run()
    }
    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val trigger            = Source.single(lastEventId)
      val merge              = builder.add(Merge[Option[String]](2))
      val getAndHandleEvents = Flow[Option[String]].map(getAndHandle)
      val unzip              = builder.add(Unzip[Future[Option[ServerSentEvent]], A]())
      val currentLastEventId = Flow[Future[Option[ServerSentEvent]]]
        .mapAsync(1)(identity)
        .scan(lastEventId)((prev, event) => event.flatMap(_.id).orElse(prev))
        .drop(1)
        .delay(retryDelay, DelayOverflowStrategy.fail) // There should be only one element in flight anyway!
      // format: OFF
      trigger ~> merge ~> getAndHandleEvents ~> unzip.in
                 merge <~ currentLastEventId <~ unzip.out0
      // format: ON
      SourceShape(unzip.out1)
    })
  }
}
