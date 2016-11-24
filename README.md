# Akka SSE #

[![Join the chat at https://gitter.im/hseeberger/akka-sse](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hseeberger/akka-sse?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/hseeberger/akka-sse.svg?branch=master)](https://travis-ci.org/hseeberger/akka-sse)

Akka SSE adds support for [Server-Sent Events](http://www.w3.org/TR/eventsource)
(SSE) – a lightweight and standardized technology for pushing notifications from
a HTTP server to a HTTP client – to
[Akka HTTP](https://github.com/akka/akka-http). In contrast to
[WebSocket](http://tools.ietf.org/html/rfc6455), which enables two-way
communication, SSE only allows for one-way communication from the server to the
client. If that's all you need, SSE offers advantages, because it's much simpler
and relies on HTTP only.

## Getting Akka SSE

Akka SSE is published to Bintray and Maven Central.

``` scala
// All releases including intermediate ones are published here,
// final ones are also published to Maven Central.
resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= Vector(
  "de.heikoseeberger" %% "akka-sse" % "2.0.0-M6",
  ...
)
```

## Usage – basics

Akka SSE models an `EventStream` as `Source[ServerSentEvent, Any]` with `Source`
from Akka Streams and `ServerSentEvent` from Akka SSE. `ServerSentEvent` is a
case class with the following fields:

- `data` of type `Option[String]`: payload, may be defined with the empty string
- `eventType` of type `Option[String]` with default `None`: handler to be
  invoked, e.g. "message", "added", etc.
- `id` of type `Option[String]` with default `None`: sets the client's last
  event ID value
- `retry` of type `Option[Int]` with default `None`: set the client's
  reconnection time

More info about the above fields can be found in the
[specification](http://www.w3.org/TR/eventsource).

## Usage – server-side

In order to respond to a HTTP request with an `EventStream`, you have to bring
the implicit `ToResponseMarshaller[EventStream]` defined by the
`EventStreamMarshalling` trait or object into the scope defining the respective
route:

``` scala
object TimeServer {

  ...

  def route = {
    import Directives._
    import EventStreamMarshalling._ // That does the trick!

    def assets = ...

    def events =
      path("events") {
        get {
          complete {
            Source
              .tick(2.seconds, 2.seconds, NotUsed)
              .map(_ => LocalTime.now())
              .map(timeToServerSentEvent)
              .keepAlive(1.second, () => ServerSentEvent.heartbeat)
          }
        }
      }

    assets ~ events
  }

  def timeToServerSentEvent(time: LocalTime): ServerSentEvent =
    ServerSentEvent(DateTimeFormatter.ISO_LOCAL_TIME.format(time))
}
```

To send periodic heartbeats, simply use the `keepAlive` standard stage with a
`ServerSentEvent.heartbeat`:

## Usage – client-side

In order to unmarshal server-sent events as `EventStream`, you have to bring the
implicit `FromEntityUnmarshaller[EventStream]`` defined by the
`EventStreamUnmarshalling` trait or object into scope:

``` scala
import EventStreamUnmarshalling._

Source.single(Get("/events"))
  .via(Http().outgoingConnection("localhost", 8000))
  .mapAsync(1)(Unmarshal(_).to[EventSource])
  .runForeach(_.runForeach(event => println(s"${LocalTime.now()} $event")))
}
```

If you want the client to reconnect to the server thereby sending the
Last-Evend-ID header if available, you can use the `EventSource`:

``` scala
val eventSource =
  EventSource("http://localhost:9000/events", Http().singleRequest(_))
eventSource.runForeach(event => println(s"${LocalTime.now()} $event"))
}
```

## References

- [Example application](https://github.com/hseeberger/akka-sse/tree/master/akka-sse-example)
- [Reactive Flows](https://github.com/hseeberger/reactive-flows)
- [Typesafe ConductR](http://www.typesafe.com/products/conductr)

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their original
author. Along with any pull requests, please state that the contribution is your
original work and that you license the work to the project under the project's
open source license. Whether or not you state this explicitly, by submitting any
copyrighted material via pull request, email, or other means you agree to
license the material under the project's open source license and warrant that
you have the legal authority to do so.

## License ##

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
