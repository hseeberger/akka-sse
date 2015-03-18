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

import akka.actor.ActorSystem
import akka.stream.ActorFlowMaterializer
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

abstract class BaseSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  implicit val mat = ActorFlowMaterializer()

  override protected def afterAll() = {
    system.shutdown()
    system.awaitTermination()
    super.afterAll()
  }
}
