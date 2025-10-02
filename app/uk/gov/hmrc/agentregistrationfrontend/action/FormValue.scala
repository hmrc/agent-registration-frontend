/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.mvc.MessagesRequest
import play.api.mvc.Request
import play.api.mvc.WrappedRequest

/** Contains validated form data extracted from [[play.api.data.Form]][T]
  *
  * @tparam T
  *   the validated form data type
  */
trait FormValue[T]:
  val formValue: T

/** A type class that defines how to combine a [[FormValue]][T] with type R.
  *
  * @tparam T
  *   the type of form value to be merged
  * @tparam R
  *   the type to merge the form value into
  */
trait MergeFormValue[R, T]:
  def mergeFormValue(
    r: R,
    formValue: T
  ): R & FormValue[T]

object MergeFormValue:

  // lowes priority given for generic requests

  given [T, A]: MergeFormValue[Request[A], T] =
    (
      r: Request[A],
      t: T
    ) =>
      new WrappedRequest[A](r)
        with FormValue[T]:
        override val formValue: T = t

  given [B, T]: MergeFormValue[MessagesRequest[B], T] =
    (
      r: MessagesRequest[B],
      t: T
    ) =>
      new MessagesRequest[B](r, r.messagesApi)
        with FormValue[T]:
        override val formValue = t
