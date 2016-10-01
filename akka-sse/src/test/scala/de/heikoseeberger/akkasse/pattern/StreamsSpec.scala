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
package pattern

import akka.Done
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSource
import akka.testkit.TestDuration
import akka.testkit.TestProbe
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Success, Try }

object StreamsSpec {

  /**
    * Provide a test source probe for an SSE flow that will receive messages for handling the
    * initial response, termination and as each SSE element is received. Only successful
    * initial responses are handled by default. Use the "onlyFailedResponses" function below
    * in order to send through failed ones.
    */
  def testSourceProbe(
      testObserver: ActorRef,
      responseHandler: (HttpResponse => Unit) => (Try[HttpResponse] => Unit) =
        Streams.onSuccess
  )(implicit system: ActorSystem,
    ec: ExecutionContext,
    mat: ActorMaterializer): TestPublisher.Probe[HttpResponse] =
    TestSource
      .probe[HttpResponse]
      .via(
        Streams.sseFlow(responseHandler(r => testObserver ! r),
                        s => testObserver ! s)
      )
      .toMat(Sink.foreach(event => testObserver ! event))(Keep.left)
      .run()

  /**
    * An initial response handler for accepting only failed responses.
    */
  def onOnlyFailedResponses(
      onResponse: HttpResponse => Unit
  )(response: Try[HttpResponse]): Unit =
    response.foreach(r => if (r.status.isFailure()) onResponse(r))
}

class StreamsSpec
    extends BaseSpec
    with ScalaFutures
    with EventStreamMarshalling {

  import StreamsSpec._

  "An sseFlow" should {

    "call its response handler when successful and its termination handler when done" in {
      val testObserver = TestProbe()

      val httpResponse = HttpResponse()

      val testSource = testSourceProbe(testObserver.ref)
      testSource.sendNext(httpResponse)
      testSource.sendComplete()

      testObserver.expectMsgPF() {
        case `httpResponse` =>
          testObserver.expectMsg(Success(Done))
        case Success(Done) =>
          testObserver.expectMsg(httpResponse)
      }
    }

    "not call its response handler when failed" in {
      val testObserver = TestProbe()

      val httpResponse = HttpResponse(status = StatusCodes.NotFound)

      val testSource = testSourceProbe(testObserver.ref)
      testSource.sendNext(httpResponse)
      testSource.sendComplete()

      testObserver.expectMsg(Success(Done))
    }

    "call its response handler when there is a response whether failed or not" in {
      val testObserver = TestProbe()

      val httpResponse = HttpResponse(status = StatusCodes.NotFound)

      val testSource = testSourceProbe(testObserver.ref, onOnlyFailedResponses)
      testSource.sendNext(httpResponse)
      testSource.sendComplete()

      testObserver.expectMsgPF() {
        case `httpResponse` =>
          testObserver.expectMsg(Success(Done))
        case Success(Done) =>
          testObserver.expectMsg(httpResponse)
      }
    }

    "pass through all non heartbeat events" in {
      val testObserver = TestProbe()

      val sseEvent = ServerSentEvent("Hello World")

      val marshallableResponse = Source
        .single(sseEvent)
        .keepAlive(1.millisecond, () => ServerSentEvent.Heartbeat): ToResponseMarshallable
      val marshalledResponse = marshallableResponse(HttpRequest()).futureValue

      val testSource = testSourceProbe(testObserver.ref)

      testSource.sendNext(marshalledResponse)
      testObserver.expectMsgPF() {
        case r: HttpResponse if r.status == StatusCodes.OK =>
      }
      testObserver.expectMsg(sseEvent)
      Thread.sleep(10.milliseconds.dilated.toMillis) // Absolutely ensure that no heartbeats come through

      testSource.sendComplete()
      testObserver.expectMsg(Success(Done))
    }
  }
}
