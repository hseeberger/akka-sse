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
package scaladsl
package unmarshalling

import akka.NotUsed
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{ Sink, Source }
import de.heikoseeberger.akkasse.javadsl.model
import de.heikoseeberger.akkasse.scaladsl.model.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent
import java.util.{ List => JList }
import org.scalatest.{ AsyncWordSpec, Matchers }
import scala.collection.JavaConverters
import scala.collection.immutable.Seq

object EventStreamUnmarshallingSpec {

  val events: Seq[ServerSentEvent] =
    1.to(666).map(n => ServerSentEvent(n.toString))

  // Also used by EventStreamUnmarshallingTest.java
  val eventsAsJava: JList[model.ServerSentEvent] = {
    import JavaConverters._
    events.map(_.asInstanceOf[model.ServerSentEvent]).asJava
  }

  // Also used by EventStreamUnmarshallingTest.java
  val entity: HttpEntity =
    HttpEntity(`text/event-stream`, Source(events).map(_.encode))
}

final class EventStreamUnmarshallingSpec extends AsyncWordSpec with Matchers with AkkaSpec {
  import EventStreamUnmarshalling._
  import EventStreamUnmarshallingSpec._

  "A HTTP entity with media-type text/event-stream" should {
    "be unmarshallable to an EventStream" in {
      Unmarshal(entity)
        .to[Source[ServerSentEvent, NotUsed]]
        .flatMap(_.runWith(Sink.seq))
        .map(_ shouldBe events)
    }
  }
}
