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

package uk.gov.hmrc.agentregistrationfrontend.util

import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc

/** A logger which is aware of the request. It will append to the message extra information such as session ID, request ID, user agent, referer, and device ID
  * etc.
  *
  * Logged messages are enriched with request-specific context.
  */
class RequestAwareLogger(
  delegateLogger: Logger
):

  def debug(message: => String)(using request: RequestHeader): Unit = logMessage(message, Debug)

  def info(message: => String)(using request: RequestHeader): Unit = logMessage(message, Info)

  def warn(message: => String)(using request: RequestHeader): Unit = logMessage(message, Warn)

  def error(message: => String)(using request: RequestHeader): Unit = logMessage(message, Error)

  def debug(
    message: => String,
    ex: Throwable
  )(using request: RequestHeader): Unit = logMessage(
    message,
    ex,
    Debug
  )

  def info(
    message: => String,
    ex: Throwable
  )(using request: RequestHeader): Unit = logMessage(
    message,
    ex,
    Info
  )

  def warn(
    message: => String,
    ex: Throwable
  )(using request: RequestHeader): Unit = logMessage(
    message,
    ex,
    Warn
  )

  def error(
    message: => String,
    ex: Throwable
  )(using request: RequestHeader): Unit = logMessage(
    message,
    ex,
    Error
  )

  private def context(using request: RequestHeader) = s"[Context: ${request.method} ${request.path}] $sessionId $requestId $userAgent $referer $deviceId"

  private def sessionId(using request: RequestHeader) = s"[SessionId: ${hc.sessionId.map(_.toString).getOrElse("")}]"

  private def requestId(using request: RequestHeader) = s"[RequestId: ${hc.requestId.map(_.toString).getOrElse("")}]"

  private def referer(using r: RequestHeader) = s"[Referer: ${r.headers.get(HeaderNames.REFERER).getOrElse("")}]"

  private def userAgent(using r: RequestHeader) = s"[UserAgent: ${r.headers.get(HeaderNames.USER_AGENT).getOrElse("")}]"

  private def deviceId(using r: RequestHeader) = s"[DeviceId: ${hc.deviceID.getOrElse("")}]"

  private def makeRichMessage(message: String)(using request: RequestHeader): String =
    request match
      case _ => s"$message $context "

  private sealed trait LogLevel

  private case object Debug
  extends LogLevel

  private case object Info
  extends LogLevel

  private case object Warn
  extends LogLevel

  private case object Error
  extends LogLevel

  private def logMessage(
    message: => String,
    level: LogLevel
  )(using request: RequestHeader): Unit =
    lazy val richMessage = makeRichMessage(message)
    level match
      case Debug => delegateLogger.debug(richMessage)
      case Info => delegateLogger.info(richMessage)
      case Warn => delegateLogger.warn(richMessage)
      case Error => delegateLogger.error(richMessage)

  private def logMessage(
    message: => String,
    ex: Throwable,
    level: LogLevel
  )(using request: RequestHeader): Unit =
    lazy val richMessage = makeRichMessage(message)
    level match
      case Debug => delegateLogger.debug(richMessage, ex)
      case Info => delegateLogger.info(richMessage, ex)
      case Warn => delegateLogger.warn(richMessage, ex)
      case Error => delegateLogger.error(richMessage, ex)
