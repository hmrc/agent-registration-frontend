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
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.*
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple

class RequestWithDataCt[
  ContentType,
  Data <: Tuple
] private (
  request: Request[ContentType],
  val data: UniqueTuple[Data]
)
extends WrappedRequest[ContentType](request):

  inline def get[T](using T PresentIn Data): T = data.get[T]

  inline def add[T](value: T)(using T AbsentIn Data): RequestWithDataCt[ContentType, T *: Data] = RequestWithDataCt.create(request, data.add(value))

  inline def update[T](value: T)(using T PresentIn Data): RequestWithDataCt[ContentType, Data] = RequestWithDataCt.create(request, data.update(value))

  inline def delete[T](using T PresentIn Data): RequestWithDataCt[ContentType, UniqueTuple.Delete[T, Data]] = RequestWithDataCt.create(request, data.delete[T])

  inline def replace[Old, New](value: New)(using
    Old PresentIn Data,
    New AbsentIn Data
  ): RequestWithDataCt[ContentType, UniqueTuple.Replace[
    Old,
    New,
    Data
  ]] = RequestWithDataCt.create(
    request,
    data.replace[Old, New](value)
  )

object RequestWithDataCt:

  def empty[ContentType](request: Request[ContentType]): RequestWithDataCt[ContentType, EmptyTuple] = apply(
    request = request,
    data = EmptyTuple
  )

  inline def apply[
    ContentType,
    Data <: Tuple
  ](
    request: Request[ContentType],
    data: Data
  ): RequestWithDataCt[ContentType, Data] = create(request, UniqueTuple(data))

  private def create[
    ContentType,
    Data <: Tuple
  ](
    request: Request[ContentType],
    data: UniqueTuple[Data]
  ): RequestWithDataCt[ContentType, Data] = new RequestWithDataCt(request, data)
