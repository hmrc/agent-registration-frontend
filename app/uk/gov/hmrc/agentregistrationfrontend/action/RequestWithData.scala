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

import play.api.mvc.Request
import play.api.mvc.WrappedRequest
import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool
import uk.gov.hmrc.agentregistrationfrontend.util.TupleToolMacros

class RequestWithData[
  A,
  Data <: Tuple
](
  val request: Request[A],
  val data: Data
)
extends WrappedRequest[A](request):

  inline def add[T](value: T)(using T AbsentIn Data): RequestWithData[A, T *: Data] = new RequestWithData(request, data.addByType(value))

  inline def get[T]: T = data.getByType[T]

  inline def update[T](value: T): RequestWithData[A, Data] = new RequestWithData(request, data.updateByType(value))

  inline def replace[Old, New](value: New): RequestWithData[A, TupleToolMacros.Replace[Data, Old, New]] =
    new RequestWithData(request, data.replaceByType[Old, New](value))

  inline def delete[T]: RequestWithData[A, TupleToolMacros.Delete[Data, T]] = new RequestWithData(request, data.deleteByType[T])

object RequestWithData:

  inline def apply[
    A,
    Data <: Tuple
  ](
    request: Request[A],
    data: Data
  ): RequestWithData[A, Data] = new RequestWithData(request, data.ensureUnique)
