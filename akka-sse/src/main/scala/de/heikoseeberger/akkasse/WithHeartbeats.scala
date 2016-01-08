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

import akka.stream.FlowShape
import akka.stream.scaladsl.{ Flow, GraphDSL, Source, MergePreferred }
import scala.concurrent.duration.FiniteDuration

/**
 * Factory for a flow of periodic heartbeats to be connected to a `Source[ServerSentEvents]`.
 */
object WithHeartbeats {

  /**
   * Factory for a with-heartbeats flow.
   *
   * @param interval duration between heartbeats
   * @return with-heartbeats flow to be connected to a `Source[ServerSentEvents]`
   */
  def apply(interval: FiniteDuration): Flow[ServerSentEvent, ServerSentEvent, Unit] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val heartbeats = builder.add(Source.tick(interval, interval, ServerSentEvent.heartbeat))
      val merge = builder.add(MergePreferred[ServerSentEvent](1, true))
      heartbeats ~> merge.in(0)
      FlowShape(merge.preferred, merge.out)
    })
}
