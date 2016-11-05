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

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.SourceShape
import akka.stream.scaladsl.{ GraphDSL, Source, Zip }
import akka.testkit.TestDuration
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class EventStreamMarshallingSpec
    extends BaseSpec
    with EventStreamMarshalling
    with EventStreamUnmarshalling {

  "A source of elements which can be viewed as ServerSentEvents" should {
    "be marshallable to a HTTP response" in {
      val elements = 1 to 666
      val marshallable =
        Source(elements).map(intToServerSentEvent): ToResponseMarshallable
      val response =
        marshallable(HttpRequest()).flatMap {
          _.entity.dataBytes
            .map(_.utf8String)
            .runFold(Vector.empty[String])(_ :+ _)
        }
      val actual   = Await.result(response, 1.second.dilated)
      val expected = elements.map(intToServerSentEvent(_).toString)
      actual shouldBe expected
    }

    "remain the same after marshalling and unmarshalling" in {
      val elements     = 1 to 666
      val expected     = Source(elements).map(intToServerSentEvent)
      val marshallable = expected: ToResponseMarshallable
      val actual = Await.result(
        marshallable(HttpRequest()).flatMap(
          response => Unmarshal(response).to[EventStream]
        ),
        1.second.dilated
      )
      val expectedAndActual = Source.fromGraph(GraphDSL.create() {
        implicit builder =>
          import GraphDSL.Implicits._
          val zip = builder.add(Zip[ServerSentEvent, ServerSentEvent]())
          expected ~> zip.in0
          actual ~> zip.in1
          SourceShape(zip.out)
      })
      val isExpectedEqualActual =
        Await.result(expectedAndActual.runFold(true) {
          case (acc, (l, r)) => acc && (l == r)
        }, 1.second.dilated)
      isExpectedEqualActual shouldBe true
    }
  }

  private def intToServerSentEvent(n: Int) = ServerSentEvent(Some(n.toString))
}
