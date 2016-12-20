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

package de.heikoseeberger.akkasse.japi;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpEntity;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;

public class EventStreamUnmarshallingTest extends JUnitSuite {

    @Test
    public void testFromEventStream() throws Exception {
        ActorSystem system = ActorSystem.create();
        try {
            Materializer mat = ActorMaterializer.create(system);

            List<ServerSentEvent> events =
                    Stream.iterate(1, n -> n + 1)
                            .limit(666)
                            .map(n -> ServerSentEvent.create("" + n))
                            .collect(Collectors.toList());
            Source<ByteString, NotUsed> data = Source.from(events).map(ServerSentEvent::encode);
            HttpEntity entity = HttpEntities.create(ContentTypes.create(MediaTypes.TEXT_EVENT_STREAM), data);

            List<ServerSentEvent> unmarshalledEvents =
                    EventStreamUnmarshalling.fromEventStream()
                            .unmarshall(entity, system.dispatcher(), mat)
                            .thenCompose(source -> source.runWith(Sink.seq(), mat))
                            .toCompletableFuture().get(3000, TimeUnit.SECONDS);

            Assert.assertEquals(events, unmarshalledEvents);
        } finally {
            system.terminate();
        }
    }
}
