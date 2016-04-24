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

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{ Broadcast, Flow, GraphDSL, Merge, Source }
import akka.stream.{ Materializer, SourceShape }
import de.heikoseeberger.commons.akka.stream.{ LastElement, ScanAhead }
import scala.concurrent.ExecutionContext

object ServerSentEventSource {
  import EventStreamUnmarshalling._

  def apply(
    responses: Flow[String, HttpResponse, Any],
    initialId: String = "1",
    lastIdToNext: String => String = s => (s.toLong + 1).toString
  )(implicit ec: ExecutionContext, mat: Materializer): Source[ServerSentEvent, Any] = {

    Source.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val trigger = Source.single(initialId)
      val merge = builder.add(Merge[String](2))
      val events = responses.mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, Any]])
      val bcast = builder.add(Broadcast[Source[ServerSentEvent, Any]](2, eagerCancel = true))
      val bs = builder.add(Flow[Source[ServerSentEvent, Any]].log("ServerSentEventSource").flatMapConcat(identity))
      val nextId = Flow[Source[ServerSentEvent, Any]]
        .flatMapConcat(_.map(_.id).via(LastElement()))
        .via(ScanAhead(Option.empty[String])((prev, current) => current.flatten.map(lastIdToNext).orElse(prev)))
        .map(_.getOrElse(initialId))
      // format: OFF
      trigger ~> merge ~> events ~> bcast ~> bs
                 merge <~ nextId <~ bcast
      // format: ON
      SourceShape(bs.out)
    })
  }
}
