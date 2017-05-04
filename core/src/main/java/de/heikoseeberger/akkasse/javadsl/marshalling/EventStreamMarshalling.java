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

package de.heikoseeberger.akkasse.javadsl.marshalling;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.RequestEntity;
import akka.stream.javadsl.Source;
import de.heikoseeberger.akkasse.javadsl.model.ServerSentEvent;

/**
 * Using `eventStreamMarshaller` lets a source of [[ServerSentEvent]]s be marshalled to a `HttpResponse`.
 */
public abstract class EventStreamMarshalling {

    public static <T> Marshaller<Source<ServerSentEvent, T>, RequestEntity> toEventStream() {
        return Marshaller.fromScala(EventStreamMarshallingConverter$.MODULE$.toEventStream());
    }
}
