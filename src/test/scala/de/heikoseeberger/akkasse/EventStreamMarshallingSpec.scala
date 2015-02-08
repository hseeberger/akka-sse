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
import akka.http.marshalling.ToResponseMarshallable
import akka.http.model.HttpRequest
import akka.stream.ActorFlowMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class EventStreamMarshallingSpec
    extends WordSpec
    with Matchers
    with BeforeAndAfterAll
    with EventStreamMarshalling {

  import system.dispatcher

  val system = ActorSystem()

  implicit val flowMaterializer = ActorFlowMaterializer()(system)

  "A source of elements which can be viewed as ServerSentEvents" should {

    "be marshallable to a HTTP response" in {
      implicit def intToServerSentEvent(n: Int): ServerSentEvent = ServerSentEvent(n.toString)
      val elements = 1 to 666
      val marshallable = Source(elements): ToResponseMarshallable
      val response = marshallable(HttpRequest()).flatMap {
        _.entity.dataBytes
          .map(_.utf8String)
          .runFold(Vector.empty[String])(_ :+ _)
      }
      val actual = Await.result(response, 1 second)
      val expected = elements.map(n => ServerSentEvent(n.toString).toString)
      actual shouldBe expected
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
  }
}
