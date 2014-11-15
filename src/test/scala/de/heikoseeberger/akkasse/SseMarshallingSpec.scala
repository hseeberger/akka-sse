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

  "TODO" should {
    "TODO" in {
      val messages = List(1, 2, 3).map(_.toString)
      val marshallable: ToResponseMarshallable = Source(messages.map(message => Sse.Message(message)))
      val request = HttpRequest()
      val response = marshallable(request).flatMap { response =>
        response.entity.dataBytes
          .map(_.utf8String)
          .fold(List.empty[String])(_ :+ _)
      }
      val actual = Await.result(response, 1 second)
      val expected = List(1, 2, 3).map(n => Sse.Message(n.toString).toString)
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
