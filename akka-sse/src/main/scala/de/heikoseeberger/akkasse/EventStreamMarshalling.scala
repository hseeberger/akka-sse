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

import akka.http.scaladsl.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.stream.scaladsl.Source
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`

/**
  * Importing [[EventStreamMarshalling.trm]] lets a source of [[ServerSentEvent]]s be marshalled to a `HttpResponse`.
  */
object EventStreamMarshalling extends EventStreamMarshalling

/**
  * Mixing in this trait lets a source of [[ServerSentEvent]]s be marshalled to a `HttpResponse`.
  */
trait EventStreamMarshalling {

  implicit final def trm: ToResponseMarshaller[Source[ServerSentEvent, Any]] =
    Marshaller.withFixedContentType(`text/event-stream`) { messages =>
      val data = messages.map(_.encode)
      HttpResponse(entity = HttpEntity.CloseDelimited(`text/event-stream`, data))
    }
}
