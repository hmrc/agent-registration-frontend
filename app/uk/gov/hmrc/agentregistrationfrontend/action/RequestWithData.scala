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

class RequestWithData[
  ContentType,
  Data <: Tuple
] private (
  val request: Request[ContentType],
  val data: Data
)
extends WrappedRequest[ContentType](request):

  inline def add[T](value: T)(using T AbsentIn Data): RequestWithData[ContentType, T *: Data] = RequestWithData.create(request, data.addByType(value))

  inline def get[T]: T = data.getByType[T]

  inline def update[T](value: T): RequestWithData[ContentType, Data] = RequestWithData.create(request, data.updateByType(value))

  inline def replace[Old, New](value: New): RequestWithData[ContentType, TupleTool.Replace[Data, Old, New]] = RequestWithData.create(
    request,
    data.replaceByType[Old, New](value)
  )

  inline def delete[T]: RequestWithData[ContentType, TupleTool.Delete[Data, T]] = RequestWithData.create(request, data.deleteByType[T])

object RequestWithData:

  inline def apply[
    ContentType,
    Data <: Tuple
  ](
    request: Request[ContentType],
    data: Data
  ): RequestWithData[ContentType, Data] = create(request, data.ensureUnique)

  private def create[
    ContentType,
    Data <: Tuple
  ](
    request: Request[ContentType],
    data: Data
  ): RequestWithData[ContentType, Data] = new RequestWithData(request, data)
