package de.heikoseeberger.akkasse

import org.openjdk.jmh.annotations.{ Benchmark, Scope, Setup, State }

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
      ServerSentEvent("line-one\nline-two\nline-three\nline-four",
                      id = Some("id")),
      ServerSentEvent("line-one", "added", "id"),
      ServerSentEvent("line-one\nline-two", "removed", "id"),
      ServerSentEvent("line-one\nline-two\nline-three\nline-four",
                      "updated",
                      "id")
  )

  @Benchmark
  def benchmark(): Unit = {
    var acc = 0L
    for (event <- Iterator.continually(events).flatten.take(100000))
      acc += event.toString.length
    sum = acc
  }
}
