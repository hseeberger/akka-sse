package de.heikoseeberger.akkasse

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ ActorMaterializer, Materializer }
import org.openjdk.jmh.annotations.{ Benchmark, Scope, Setup, State, TearDown }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ServerSentEventParserBenchmark {
  case class State(system: ActorSystem, mat: Materializer)
}

@State(Scope.Benchmark)
class ServerSentEventParserBenchmark {

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
    implicit val system = state.system
    implicit val mat    = state.mat
    val done = Source
      .fromIterator(
          () =>
            Iterator
              .fill(50000)(Vector("event:foo", "data:bar", "data:baz", ""))
      )
      .mapConcat(identity)
      .take(50000)
      .via(new ServerSentEventParser(1048576))
      .runForeach(_ => ())
    Await.ready(done, Duration.Inf)
  }
}
