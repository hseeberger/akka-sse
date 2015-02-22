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
package stream

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class EventPublisherSpec
    extends WordSpec
    with Matchers
    with BeforeAndAfterAll {

  val system = ActorSystem()

  implicit val flowMaterializer = ActorFlowMaterializer()(system)

  "An EventPublisher" should {

    "be useful as a Source" in {
      val eventPublisher = system.actorOf(Props(new EventPublisher[Int](10) {
        context.system.scheduler.scheduleOnce(1 second, self, "stop")(context.dispatcher)
        override protected def receiveEvent = {
          case n: Int =>
            onEvent(n)
          case "stop" =>
            onComplete()
            context.stop(self)
        }
      }))
      for (n <- 1 to 20) eventPublisher ! n
      // As we are sending before materializing the flow, only the 10 last messages get buffered
      Await.result(Source(ActorPublisher[Int](eventPublisher)).runFold(0)(_ + _), 2 seconds) shouldBe (11 to 20).sum
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
  }
}
