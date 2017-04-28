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

package de.heikoseeberger.akkasse.japi.headers;

/**
 * The Last-Event-ID header is sent by a client to the server to signal the ID of the last
 * sever-sent event received.
 */
public interface LastEventId {

    /**
     * Creates a Last-Event-ID header.
     *
     * @param value value of the last event ID, encoded as UTF-8 string
     */
    public static LastEventId create(String value) {
        return de.heikoseeberger.akkasse.headers.Last$minusEvent$minusID$.MODULE$.apply(value);
    }

    public abstract String getValue();
}
