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

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.ByteString
import org.openjdk.jmh.annotations.{ Benchmark, Scope, Setup, State, TearDown }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object LineParserBenchmark {
  case class State(system: ActorSystem, mat: Materializer)
}

@State(Scope.Benchmark)
class LineParserBenchmark {

  private var state: LineParserBenchmark.State = null

  @Setup
  def setup(): Unit = {
    val system = ActorSystem()
    val mat    = ActorMaterializer()(system)
    state = LineParserBenchmark.State(system, mat)
  }

  @TearDown
  def tearDown(): Unit = Await.ready(state.system.terminate(), Duration.Inf)

  @Benchmark
  def benchmark(): Unit = {
    implicit val system    = state.system
    implicit val mat       = state.mat
    def next(last: String) = if (last == "event:foo\ndata:") "bar\ndata:baz\n\n" else "event:foo\ndata:"
    val done =
      Source
        .fromIterator(() => Iterator.iterate("event:foo\ndata:")(next))
        .map(ByteString(_))
        .take(50000)
        .via(new LineParser(1048576))
        .runForeach(_ => ())
    Await.ready(done, Duration.Inf)
  }
}
