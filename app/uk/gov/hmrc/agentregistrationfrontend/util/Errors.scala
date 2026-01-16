/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.util

import play.api.http.Status
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.net.URL
import scala.concurrent.Future

object Errors
extends RequestAwareLogging:

  export uk.gov.hmrc.agentregistration.shared.util.Errors.*

  inline def throwBadRequestException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.BAD_REQUEST
    )

  inline def throwBadRequestExceptionF(message: => String)(using request: RequestHeader): Future[Nothing] =
    logger.error(message)
    Future.failed(UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.BAD_REQUEST
    ))

  inline def throwNotFoundException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.NOT_FOUND
    )

  inline def throwServerErrorException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.INTERNAL_SERVER_ERROR
    )

  inline def notImplemented(message: => String = "")(using request: RequestHeader): Nothing =
    val m = s"Unimplemented: $message"
    logger.error(m)
    throw UpstreamErrorResponse(m, play.mvc.Http.Status.NOT_IMPLEMENTED)

  def throwUpstreamErrorResponse(
    httpMethod: String,
    url: URL,
    status: Int,
    response: => HttpResponse,
    info: String = ""
  ): Nothing =
    throw UpstreamErrorResponse(
      message =
        info + "; " + httpErrorFunctions.upstreamResponseMessage(
          httpMethod,
          url.toString,
          status,
          response.body
        ),
      statusCode = status,
      reportAs =
        if status === Status.BAD_GATEWAY
        then Status.BAD_GATEWAY
        else Status.INTERNAL_SERVER_ERROR,
      headers = response.headers
    )

  val httpErrorFunctions: HttpErrorFunctions = new HttpErrorFunctions {}
