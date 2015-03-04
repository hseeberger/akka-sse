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

import akka.http.marshalling.{ Marshaller, ToResponseMarshaller }
import akka.http.model.{ HttpCharsets, HttpEntity, HttpResponse }
import akka.stream.scaladsl.Source
import scala.concurrent.ExecutionContext

/**
 * Importing [[EventStreamMarshalling.toResponseMarshaller]] lets an `akka.stream.scaladsl.Source` of elements
 * which can be viewed as [[ServerSentEvent]]s be marshallable to a `akka.http.model.HttpResponse`.
 */
object EventStreamMarshalling extends EventStreamMarshalling

/**
 * Mixing in this trait lets an `akka.stream.scaladsl.Source` of elements
 * which can be viewed as [[ServerSentEvent]]s be marshallable to a `akka.http.model.HttpResponse`.
 */
trait EventStreamMarshalling {

  type ToServerSentEvent[A] = A => ServerSentEvent

  implicit final def toResponseMarshaller[A: ToServerSentEvent, B](implicit ec: ExecutionContext): ToResponseMarshaller[Source[A, B]] =
    Marshaller.withFixedCharset(MediaTypes.`text/event-stream`, HttpCharsets.`UTF-8`) { messages =>
      val data = messages.mapMaterialized(_ => ()).map(_.toByteString) // TODO mapMaterialized might become obsolete once https://github.com/akka/akka/issues/16933 is fixed!
      HttpResponse(entity = HttpEntity.CloseDelimited(MediaTypes.`text/event-stream`, data))
    }

  /**
   * Convert a [[Source]] of user events to a [[Source]] of [[ServerSentEvent]].
   * Useful when combining such sources in akka-stream FlowGraphs.
   */
  implicit final def toServerSentEventStream[A: ToServerSentEvent, M](source: Source[A, M]): Source[ServerSentEvent, M] =
    source.map(implicitly[ToServerSentEvent[A]])
}
