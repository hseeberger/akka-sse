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

import java.util.{ Optional, OptionalInt }

private object OptionConverter {

  implicit class OptionOps[A](val option: Option[A]) extends AnyVal {

    def toOptional: Optional[A] =
      option.fold(Optional.empty[A]())(Optional.of)

    def toOptionalInt[B >: A <: Int]: OptionalInt =
      option.asInstanceOf[Option[B]].fold(OptionalInt.empty())(OptionalInt.of)
  }

  def toOption[A](optional: Optional[A]): Option[A] =
    if (optional.isPresent) Some(optional.get()) else None

  def toOption(optional: OptionalInt): Option[Int] =
    if (optional.isPresent) Some(optional.getAsInt) else None
}
