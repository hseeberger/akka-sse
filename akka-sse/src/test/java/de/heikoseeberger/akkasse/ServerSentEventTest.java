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

package de.heikoseeberger.akkasse;

import org.junit.Assert;
import org.junit.Test;
import org.scalatest.junit.JUnitSuite;
import scala.Some;

public class ServerSentEventTest extends JUnitSuite {

    @Test
    public void createData() {
        final ServerSentEvent event = ServerSentEvent.create("data");
        Assert.assertEquals("data", event.data());
    }

    @Test
    public void createDataEvent() {
        final ServerSentEvent event = ServerSentEvent.create("data", "eventType");
        Assert.assertEquals("data", event.data());
        Assert.assertEquals(Some.apply("eventType"), event.eventType());
    }
}
