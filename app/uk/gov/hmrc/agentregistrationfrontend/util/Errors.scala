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

import play.api.mvc.Request
import play.api.mvc.RequestHeader
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

object Errors
extends RequestAwareLogging:

  /** Creates a requirement which has to pass in order to continue computation. If it fails it will result in Upstream4xxResponse.
    */
  def require(
    requirement: Boolean,
    message: => String
  )(using request: RequestHeader): Unit =
    if !requirement then
      logger.error(s"Requirement failed: $message")
      throw UpstreamErrorResponse(message, play.mvc.Http.Status.BAD_REQUEST)
    else ()

  def requireF(
    requirement: Boolean,
    message: => String
  )(using request: Request[?]): Future[Unit] =
    if !requirement then
      logger.error(s"Requirement failed: $message")
      Future.failed(UpstreamErrorResponse(message, play.mvc.Http.Status.BAD_REQUEST))
    else Future.successful(())

  @inline def throwBadRequestException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.BAD_REQUEST
    )

  @inline def throwBadRequestExceptionF(message: => String)(using request: RequestHeader): Future[Nothing] =
    logger.error(message)
    Future.failed(UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.BAD_REQUEST
    ))

  @inline def throwNotFoundException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.NOT_FOUND
    )

  @inline def throwServerErrorException(message: => String)(using request: RequestHeader): Nothing =
    logger.error(message)
    throw UpstreamErrorResponse(
      message,
      play.mvc.Http.Status.INTERNAL_SERVER_ERROR
    )

  /** Call this to ensure that we don't do stupid things, like make illegal transitions (eg. from Finished to New)
    */
  def sanityCheck(
    requirement: Boolean,
    message: => String
  )(using request: RequestHeader): Unit =
    if !requirement then
      logger.error(message)
      throw UpstreamErrorResponse(message, play.mvc.Http.Status.INTERNAL_SERVER_ERROR)
    else ()

  def notImplemented(message: => String = "")(using request: RequestHeader): Nothing =
    val m = s"Unimplemented: $message"
    logger.error(m)
    throw UpstreamErrorResponse(m, play.mvc.Http.Status.NOT_IMPLEMENTED)
