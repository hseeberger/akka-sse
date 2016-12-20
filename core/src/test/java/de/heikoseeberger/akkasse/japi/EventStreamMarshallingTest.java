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

import akka.http.javadsl.server.Route;
import akka.http.javadsl.testkit.JUnitRouteTest;
import akka.http.javadsl.testkit.TestRouteResult;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static akka.http.javadsl.model.HttpRequest.GET;
import static de.heikoseeberger.akkasse.japi.MediaTypes.TEXT_EVENT_STREAM;

public class EventStreamMarshallingTest extends JUnitRouteTest {

    @Test
    public void testToEventStream() {
        final List<ServerSentEvent> events = new ArrayList<>();
        events.add(ServerSentEvent.create("1"));
        events.add(ServerSentEvent.create("2"));
        final Route route = completeOK(Source.from(events), EventStreamMarshalling.toEventStream());

        final ByteString expectedEntity = events
                .stream()
                .map(e -> ((de.heikoseeberger.akkasse.ServerSentEvent) e).encode())
                .reduce(ByteString.empty(), ByteString::$plus$plus);
        final TestRouteResult routeResult = testRoute(route).run(GET("/"));
        routeResult.assertMediaType(TEXT_EVENT_STREAM);
        routeResult.assertEquals(expectedEntity, routeResult.entityBytes(), "Entity should carry events!");
    }
}
