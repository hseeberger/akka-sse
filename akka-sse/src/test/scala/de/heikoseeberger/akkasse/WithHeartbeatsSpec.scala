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

import akka.stream.scaladsl.{ FlowGraph, Source, Zip }
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class WithHeartbeatsSpec extends BaseSpec {

  "Heartbeats" should {
    "emit elements from the attached source and heartbeats" in {
      val events = Source() { implicit builder =>
        import FlowGraph.Implicits._
        val ticks = builder.add(Source(100.millis, 200.millis, None))
        val numbers = builder.add(Source(() => Iterator.from(1)))
        val zip = builder.add(Zip[None.type, Int]())
        ticks ~> zip.in0
        numbers ~> zip.in1
        zip.out.map(pair => intToServerSentEvent(pair._2)).outlet
      }
      val result = events
        .via(WithHeartbeats(200.millis))
        .take(10)
        .runFold(Vector.empty[ServerSentEvent])(_ :+ _)
      val actual = Await.result(result, 2.seconds)
      val expected = 1.to(5).to[Vector].flatMap { n => Vector(intToServerSentEvent(n), ServerSentEvent.heartbeat) }
      actual shouldBe expected
    }
  }

  def intToServerSentEvent(n: Int): ServerSentEvent = ServerSentEvent(n.toString)
}
