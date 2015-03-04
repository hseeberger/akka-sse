/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkasse

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import scala.concurrent.duration._

object Heartbeats {

  /**
   * Source of periodic heartbeats.
   * @param interval determines the interval of heartbeats.
   */
  def every(interval: FiniteDuration): Source[ServerSentEvent, Unit] =
    if (interval > 0.seconds)
      Source(0.seconds, interval, ServerSentEvent.Heartbeat).mapMaterialized(_ => ())
    else
      Source.empty[ServerSentEvent]
}
