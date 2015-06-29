# akka-sse #

[![Join the chat at https://gitter.im/hseeberger/akka-sse](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/hseeberger/akka-sse?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/hseeberger/akka-sse.svg?branch=master)](https://travis-ci.org/hseeberger/akka-sse)

akka-sse adds support for [Server-Sent Events](http://www.w3.org/TR/eventsource) (SSE) – a lightweight and standardized technology for pushing notifications from a HTTP server to a HTTP client – to akka-http. In contrast to [WebSocket](http://tools.ietf.org/html/rfc6455), which enables two-way communication, SSE only allows for one-way communication from the server to the client. If that's all you need, SSE offers advantages, because it's much simpler and relies on HTTP only.

The latest release of akka-sse is 1.0.0-RC2 and depends on akka-http 1.0-RC4.

## Getting akka-sse

akka-sse is published to Bintray and Maven Central.

``` scala
// All releases including intermediate ones are published here,
// final ones are also published to Maven Central.
resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= List(
  "de.heikoseeberger" %% "akka-sse" % "1.0.0-RC2",
  ...
)
```

## Usage – basics

akka-sse models server-sent events as `Source[ServerSentEvent, Any]` with `Source` from Akka Streams and `ServerSentEvent` from akka-sse. `ServerSentEvent` is a case class with the following fields:

- `data` of type `String`: payload, may be empty
- `eventType` of type `Option[String]` with default `None`: handler to be invoked, e.g. "message" (default), "added", etc.
- `id` of type `Option[String]` with default `None`: sets the client's last event ID string
- `retry` of type `Option[Int]` with default `None`: set the client's reconnection time

More info about the above fields can be found in the  [Server-Sent Events specification](http://www.w3.org/TR/eventsource).

## Usage – server-side

In order to produce server-sent events on the server as a response to a HTTP request, you have to bring the implicit `toResponseMarshaller` defined by the `EventStreamMarshalling` trait or object into scope where you define your respective route. Then you complete the HTTP request with a `Source[ServerSentEvent]`:

``` scala
object TimeServer {

  ...

  def route(system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
    import Directives._
    import EventStreamMarshalling._
    get {
      complete {
        val timeEventPublisher = system.actorOf(TimeEventPublisher.props)
        Source(ActorPublisher[ServerSentEvent](timeEventPublisher))
      }
    }
  }
}
```

Like shown above, in order to ease creating a `Source[ServerSentEvent]` you can extend an `EventPublisher[A: ToServerSentEvent]` which itself extends `ActorPublisher[ServerSentEvent]`. As you can see from the context bound on the type parameter of `EventPublisher`, an implicit value of `ToServerSentEvent[A]`, which translates to `A => ServerSentEvent`, has to be in scope – in other words there has to be an implicit conversion from `A` to `ServerSentEvent`. Within an `EventPublisher` a server-sent event is published – given that the stream is active and there is demand – by calling `onEvent`:

``` scala
object TimeServer {

  object TimeEventPublisher {
    def props: Props = Props(new TimeEventPublisher)
  }

  class TimeEventPublisher extends EventPublisher[LocalTime](10, 1.second) {
    import context.dispatcher

    context.system.scheduler.schedule(2.seconds, 2.seconds, self, "now")

    override protected def receiveEvent = {
      case "now" => onEvent(LocalTime.now())
    }
  }

  implicit def dateTimeToServerSentEvent(time: LocalTime): ServerSentEvent = ServerSentEvent(
    DateTimeFormatter.ISO_LOCAL_TIME.format(time)

  ...
}
```

An `EventPublisher` is parameterized with a `bufferSize` of type `Int` and a `heartbeatInterval` of type `Duration` with a default value of `Duration.Undefined`. The `bufferSize` is needed, because as long as the stream is not active or when there's no demand from downstream, the `EventPublisher` buffers events. If the `bufferSize` is reached and a new event received, the oldest event is discarded. If the `heartbeatInterval` is defined, a `ServerSentEvent.heartbeat` will be published if no other event has been received within that interval.

## Usage – client-side

In order to consume server-sent events on the client as part of a HTTP response, you have to bring the implicit `fromEntityUnmarshaller` defined by the `EventStreamUnmarshalling` trait or object into scope where you define your response handling.

``` scala
object TimeClient {
  import EventStreamUnmarshalling._

  ...

  Source.single(Get())
    .via(Http().outgoingConnection("127.0.0.1", 9000))
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

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
