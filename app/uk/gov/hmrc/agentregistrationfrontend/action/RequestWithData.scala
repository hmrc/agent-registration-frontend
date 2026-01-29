/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.action

import uk.gov.hmrc.agentregistrationfrontend.util.TupleMacros
import scala.compiletime.*
import play.api.mvc.Request
import play.api.mvc.WrappedRequest

class RequestWithData[
  A,
  Data <: Tuple
](
  val request: Request[A],
  val data: Data // Data is a tuple
)
extends WrappedRequest[A](request):

  inline def get[T]: T =
    inline if constValue[TupleMacros.IsMember[Data, T]] then
      find[Data, T](data)
    else
      fail[Data, T]

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private inline def find[Tup, E](t: Any): E =
    inline erasedValue[Tup] match
      case _: (E *: tail) => t.asInstanceOf[E *: tail].head
      case _: (h *: tail) => find[tail, E](t.asInstanceOf[h *: tail].tail)
      case _ => error("Type not found in tuple")

  private inline def fail[Data, T]: Nothing = ${ TupleMacros.failImpl[Data, T] }
