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

package de.heikoseeberger.akkasse.japi;

import akka.util.ByteString;
import java.util.Optional;
import java.util.OptionalInt;
import static scala.compat.java8.OptionConverters.toScala;

/**
 * Representation of a server-sent event.
 */
public abstract class ServerSentEvent {

    /**
     * Creates a [[ServerSentEvent]].
     *
     * @param data data, may be empty or span multiple lines
     */
    public static ServerSentEvent create(String data) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data);
    }

    /**
     * Creates a [[ServerSentEvent]].
     *
     * @param retry reconnection delay in milliseconds
     */
    public static ServerSentEvent create(int retry) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(retry);
    }

    /**
     * Creates a [[ServerSentEvent]].
     *
     * @param data data, may be empty or span multiple lines
     * @param type type, must not contain \n or \r
     */
    public static ServerSentEvent create(String data, String type) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data, type);
    }

    /**
     * Creates a [[ServerSentEvent]].
     *
     * @param data data, may be empty or span multiple lines
     * @param type type, must not contain \n or \r
     * @param id id, must not contain \n or \r
     */
    public static ServerSentEvent create(String data, String type, String id) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(data, type, id);
    }

    /**
     * Creates a [[ServerSentEvent]].
     *
     * @param data optional data, may be empty or span multiple lines
     * @param type optional type, must not contain \n or \r
     * @param id optional id, must not contain \n or \r
     * @param retry optional reconnection delay in milliseconds
     */
    public static ServerSentEvent create(Optional<String> data,
                                         Optional<String> type,
                                         Optional<String> id,
                                         OptionalInt retry) {
        return de.heikoseeberger.akkasse.ServerSentEvent.apply(
                toScala(data),
                toScala(type),
                toScala(id),
                toScala(retry)
        );
    }

    /**
     * Optional data, may be empty or span multiple lines.
     */
    public abstract Optional<String> getData();

    /**
     * Optional type, must not contain \n or \r.
     */
    public abstract Optional<String> getType();

    /**
     * Optional id, must not contain \n or \r.
     */
    public abstract Optional<String> getId();

    /**
     * Optional reconnection delay in milliseconds.
     */
    public abstract OptionalInt getRetry();

    /**
     * Encodes this server-sent event to an `akka.util.ByteString` according to the
     * [[http://www.w3.org/TR/eventsource/#event-stream-interpretation SSE specification]].
     *
     * @return message converted to UTF-8 encoded `akka.util.ByteString`
     */
    public abstract ByteString encode();
}
