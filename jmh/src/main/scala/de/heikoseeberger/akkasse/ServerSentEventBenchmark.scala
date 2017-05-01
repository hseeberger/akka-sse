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

import org.openjdk.jmh.annotations.{ Benchmark, Scope, State }

@State(Scope.Benchmark)
class ServerSentEventBenchmark {

  private var sum = 0L

  private val events = Vector(
    ServerSentEvent("line-one"),
    ServerSentEvent("line-one\nline-two"),
    ServerSentEvent("line-one\nline-two\nline-three\nline-four"),
    ServerSentEvent("line-one", "added"),
    ServerSentEvent("line-one\nline-two", "removed"),
    ServerSentEvent("line-one\nline-two\nline-three\nline-four", "updated"),
    ServerSentEvent("line-one", id = Some("id")),
    ServerSentEvent("line-one\nline-two", id = Some("id")),
    ServerSentEvent("line-one\nline-two\nline-three\nline-four", id = Some("id")),
    ServerSentEvent("line-one", "added", "id"),
    ServerSentEvent("line-one\nline-two", "removed", "id"),
    ServerSentEvent("line-one\nline-two\nline-three\nline-four", "updated", "id")
  )

  @Benchmark
  def benchmark(): Unit = {
    var acc = 0L
    for (event <- Iterator.continually(events).flatten.take(100000)) acc += event.encode.length
    sum = acc
  }
}
