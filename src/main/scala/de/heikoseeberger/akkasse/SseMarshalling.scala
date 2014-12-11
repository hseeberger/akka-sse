/*
 * Copyright 2014 Heiko Seeberger
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
 * Mixing in this trait lets an `akka.stream.scaladsl.Source` of elements
 * which can be viewed as [[Sse.Message]]s be marshallable to a `akka.http.model.HttpResponse`.
 */
trait SseMarshalling {

  type ToSseMessage[A] = A => Sse.Message

  implicit def toResponseMarshaller[A: ToSseMessage](implicit ec: ExecutionContext): ToResponseMarshaller[Source[A]] =
    Marshaller.withFixedCharset(Sse.`text/event-stream`.mediaType, HttpCharsets.`UTF-8`) { messages =>
      val entity = HttpEntity.CloseDelimited(
        Sse.`text/event-stream`.mediaType,
        messages.map(_.toByteString)
      )
      HttpResponse(entity = entity)
    }
}

/**
 * Importing [[SseMarshalling.toResponseMarshaller]] lets an `akka.stream.scaladsl.Source` of elements
 * which can be viewed as [[Sse.Message]]s be marshallable to a `akka.http.model.HttpResponse`.
 */
object SseMarshalling extends SseMarshalling
