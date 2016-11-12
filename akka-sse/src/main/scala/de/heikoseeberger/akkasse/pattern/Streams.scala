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

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Sink }
import akka.{ Done, NotUsed }
import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Common streaming patterns for SSE
  */
@deprecated("Consider using EventStreamClient instead", "2.0.0")
object Streams {

  /**
    * Provides a flow representing a common pipeline for handling the
    * establishment of an SSE event stream and its termination. The flow
    * produces SSE elements. This flow is intended to be used in conjunction
    * with having established a connection and made a request. For example:
    *
    * {{{
    * Source
    *   .single(myRequest)
    *   .via(myConnection)
    *   .via(
    *     sseFlow(
    *       onSuccess { httpResponse =>
    *         // Do something given a successful establishment of an event stream
    *       },
    *       { outcome =>
    *         // Do something given that the event stream has terminated
    *       }
    *     )
    *   )
    *   .runForeach { sseEvent =>
    *     // Do something for each event
    *   }
    * }}}
    *
    * @param onResponse the initial response handler
    * @param onTermination the handler to call when the connection has been terminated
    * @param ec an execution context for certain parts of the flow mappings
    * @param mat a materializer for certain parts of the flow
    * @return a flow that emits ServerSentEvent elements
    */
  def sseFlow(onResponse: Try[HttpResponse] => Unit,
              onTermination: Try[Done] => Unit)(
      implicit ec: ExecutionContext,
      mat: ActorMaterializer
  ): Flow[HttpResponse, ServerSentEvent, NotUsed] = {
    import de.heikoseeberger.akkasse.client.EventStreamUnmarshalling._
    Flow[HttpResponse]
      .alsoTo(Sink.head.mapMaterializedValue(_.onComplete(onResponse)))
      .alsoTo(Sink.onComplete(onTermination))
      .mapAsync(1)(Unmarshal(_).to[EventStream])
      .flatMapConcat(identity)
  }

  /**
    * A convenience for wrapping onResponse handlers where the  event
    * stream HTTP response is successful.
    *
    * @param onResponse the handler of the successful HTTP response
    * @param response the response passed in by the sseFlow, which can have failed
    */
  def onSuccess(onResponse: HttpResponse => Unit)(
      response: Try[HttpResponse]): Unit =
    for (r <- response if r.status.isSuccess) onResponse(r)
}
