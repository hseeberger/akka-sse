# Akka SSE #

[![Join the chat at https://gitter.im/hseeberger/akka-sse](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hseeberger/akka-sse?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/hseeberger/akka-sse.svg?branch=master)](https://travis-ci.org/hseeberger/akka-sse)

Akka SSE adds support for [Server-Sent Events](http://www.w3.org/TR/eventsource) (SSE) – a lightweight and standardized
technology for pushing notifications from a HTTP server to a HTTP client – to akka-http. In contrast to
[WebSocket](http://tools.ietf.org/html/rfc6455), which enables two-way communication, SSE only allows for one-way
communication from the server to the client. If that's all you need, SSE offers advantages, because it's much simpler
and relies on HTTP only.

## Getting Akka SSE

Akka SSE is published to Bintray and Maven Central.

``` scala
// All releases including intermediate ones are published here,
// final ones are also published to Maven Central.
resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= List(
  "de.heikoseeberger" %% "akka-sse" % "1.7.5",
  ...
)
```

## Usage – basics

Akka SSE models server-sent events as `Source[ServerSentEvent, A]` with `Source` from Akka Streams,
`ServerSentEvent` from Akka SSE and `A` an arbitrary type on the server-side and `Any` on the client-side.

`ServerSentEvent` is a case class with the following fields:

- `data` of type `String`: payload, may be empty
- `eventType` of type `Option[String]` with default `None`: handler to be invoked, e.g. "message", "added", etc.
- `id` of type `Option[String]` with default `None`: sets the client's last event ID string
- `retry` of type `Option[Int]` with default `None`: set the client's reconnection time

More info about the above fields can be found in the [specification](http://www.w3.org/TR/eventsource).

## Usage – server-side

In order to produce server-sent events on the server as a response to a HTTP request, you have to bring the implicit
`toResponseMarshaller` defined by the `EventStreamMarshalling` trait or object into scope where you define your
respective route. Then you complete the HTTP request with a `Source[ServerSentEvent, A]` for an arbitrary `A`:

``` scala
object TimeServer {

  ...

  def route = {
    import Directives._
    import EventStreamMarshalling._
    get {
      complete {
        Source.tick(2.seconds, 2.seconds, ())
          .map(_ => LocalTime.now())
          .map(dateTimeToServerSentEvent)
      }
    }
  }
}
```

If you need periodic heartbeats, simply use the `keepAlive` standard stage with a `ServerSentEvent.heartbeat`:

``` scala
Source.tick(2.seconds, 2.seconds, Unit)
  .map(_ => LocalTime.now())
  .map(dateTimeToServerSentEvent)
  .keepAlive(1.second, () => ServerSentEvent.heartbeat)
}
```

## Usage – client-side

In order to unmarshal server-sent events as `Source[ServerSentEvent, Any]`, you have to bring the implicit
`fromEntityUnmarshaller` defined by the `EventStreamUnmarshalling` trait or object into scope where you define your
response handling.

``` scala
object TimeClient {
  import EventStreamUnmarshalling._

  ...

  Source.single(Get())
    .via(Http().outgoingConnection("127.0.0.1", 8000))
    .mapAsync(1)(Unmarshal(_).to[Source[ServerSentEvent, Any]])
    .runForeach(_.runForeach(event => println(s"${LocalTime.now()} $event")))
}
```

## References

- [Example application](https://github.com/hseeberger/akka-sse/tree/master/akka-sse-example)
- [Reactive Flows](https://github.com/hseeberger/reactive-flows)
- [Typesafe ConductR](http://www.typesafe.com/products/conductr)

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original author. Along with any pull requests, please state that the contribution is your original work and that you license the work to the project under the project's open source license. Whether or not you state this explicitly, by submitting any copyrighted material via pull request, email, or other means you agree to license the material under the project's open source license and warrant that you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
