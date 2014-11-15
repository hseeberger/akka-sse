/*
 * Copyright 2014 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkasse

import akka.actor.ActorSystem
import akka.http.marshalling.{ Marshaller, ToResponseMarshallable }
import akka.http.model.HttpRequest
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Source
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SseMarshallingSpec
    extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with TypeCheckedTripleEquals
    with SseMarshalling {

  import system.dispatcher

  "A source of elements which can be viewed as SSS messages" should {
    "be marshallable to a HTTP response" in {
      implicit def itnToSseMessage(n: Int): Sse.Message =
        Sse.Message(n.toString)
      val elements = 1 to 666
      val marshallable: ToResponseMarshallable = Source(elements)
      val response = marshallable(HttpRequest()).flatMap { response =>
        response.entity.dataBytes
          .map(_.utf8String)
          .fold(Vector.empty[String])(_ :+ _)
      }
      val actual = Await.result(response, 1 second)
      val expected = elements.map(n => Sse.Message(n.toString).toString)
      actual should ===(expected)
    }
  }

  lazy val system = ActorSystem()

  implicit lazy val flowMaterializer = FlowMaterializer()(system)

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
  }
}
