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
package javadsl
package marshalling

import akka.http.javadsl.model.RequestEntity
import akka.http.scaladsl.marshalling.Marshaller
import akka.stream.javadsl.Source
import de.heikoseeberger.akkasse.javadsl.model.ServerSentEvent

private object EventStreamMarshallingConverter {

  final def toEventStream[A]: Marshaller[Source[ServerSentEvent, A], RequestEntity] = {
    def asScala(eventStream: Source[ServerSentEvent, A]) =
      eventStream.asScala.map(_.asInstanceOf[scaladsl.model.ServerSentEvent])
    scaladsl.marshalling.EventStreamMarshalling.toEventStream.compose(asScala)
  }
}
