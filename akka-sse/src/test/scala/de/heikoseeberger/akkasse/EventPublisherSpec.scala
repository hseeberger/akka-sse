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

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class EventPublisherSpec extends BaseSpec {

  "An EventPublisher" should {

    "receive 20 elements, discard the first 10 given a buffer size of 10 and then close down with the sink having closed" in {
      val eventPublisher = system.actorOf(Props(new EventPublisher[Int](10) {
        override protected def receiveEvent = {
          case n: Int => onEvent(n)
        }
      }))
      for (n <- 1 to 20) eventPublisher ! n
      Await.result(Source(ActorPublisher[Int](eventPublisher)).take(10).runFold(0)(_ + _), 2.seconds) shouldBe (11 to 20).sum

      val watcher = TestProbe()
      watcher.watch(eventPublisher)
      watcher.expectTerminated(eventPublisher)
    }
  }
}
