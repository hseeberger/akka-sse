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
    val mat = ActorMaterializer()(system)
    state = LineParserBenchmark.State(system, mat)
  }

  @TearDown
  def tearDown(): Unit = Await.ready(state.system.terminate(), Duration.Inf)

  @Benchmark
  def benchmark(): Unit = {
    implicit val system = state.system
    implicit val mat = state.mat
    def next(last: String) = if (last == "event:foo\ndata:") "bar\ndata:baz\n\n" else "event:foo\ndata:"
    val done = Source
      .fromIterator(() => Iterator.iterate("event:foo\ndata:")(next))
      .map(ByteString(_))
      .take(50000)
      .via(new LineParser(1048576))
      .runForeach(_ => ())
    Await.ready(done, Duration.Inf)
  }
}
