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
package japi

import akka.http.javadsl.model.HttpResponse
import akka.http.scaladsl.marshalling.Marshaller
import akka.stream.javadsl.Source
import de.heikoseeberger.akkasse.EventStreamMarshalling.trm

private object EventStreamMarshallingConverter {

  final def jtrm[A]: Marshaller[Source[ServerSentEvent, A], HttpResponse] =
    trm.compose(_.asScala.map(_.asInstanceOf[de.heikoseeberger.akkasse.ServerSentEvent]))
}
