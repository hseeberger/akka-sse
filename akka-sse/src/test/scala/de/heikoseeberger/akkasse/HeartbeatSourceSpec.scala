/*
 * Copyright 2015 Heiko Seeberger
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

import akka.actor.{ ActorSystem, Props }
import akka.stream.ActorFlowMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{ FlowGraph, Merge, Sink, Source }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Span }
import scala.concurrent.duration._

class HeartbeatSourceSpec extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit val system = ActorSystem()
  implicit val flowMaterializer = ActorFlowMaterializer()

  "heart beats" should {

    "be merged into event stream" in {
      import de.heikoseeberger.akkasse.EventStreamMarshalling._
      implicit def intToServerSentEvent(n: Int): ServerSentEvent = ServerSentEvent(n.toString)

      val eventPublisher = system.actorOf(Props(new EventPublisher[Int](10) {
        override protected def receiveEvent = {
          case n: Int => onEvent(n)
        }
      }))
      val events = Source(ActorPublisher[Int](eventPublisher))
      val heartbeats = Heartbeats.every(50 millis)

      val source = Source() { implicit b =>
        import FlowGraph.Implicits._

        val merge = b.add(Merge[ServerSentEvent](inputPorts = 2))
        merge <~ events
        merge <~ heartbeats
        merge.out
      }

      for (n <- 1 to 4) eventPublisher ! 42

      whenReady(source.grouped(8).runWith(Sink.head()), Timeout(scaled(Span(300, Millis)))) { result =>
        val events = result.groupBy(_.data)
        events("42").size shouldBe 4
        events("heartbeat").size shouldBe 4
      }
    }

  }

  override protected def afterAll() = {
    super.afterAll()
    system.shutdown()
    system.awaitTermination()
  }

}
