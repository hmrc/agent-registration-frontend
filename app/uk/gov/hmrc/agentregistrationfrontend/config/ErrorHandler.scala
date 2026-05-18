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

package uk.gov.hmrc.agentregistrationfrontend.config

import play.api.PlayException
import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.twirl.api.Html
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.views.html.ErrorTemplate
import uk.gov.hmrc.mdc.RequestMdc
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
  errorTemplate: ErrorTemplate,
  override val messagesApi: MessagesApi
)(using override val ec: ExecutionContext)
extends FrontendErrorHandler,
  RequestAwareLogging:

  override def standardErrorTemplate(
    pageTitle: String,
    heading: String,
    message: String
  )(using request: RequestHeader): Future[Html] = Future.successful(errorTemplate(
    pageTitle,
    heading,
    message
  ))

  /** Mimics standard error handler, the only difference is that it uses RequestAwareLogger to log request-aware context.
    */
  override def onServerError(
    request: RequestHeader,
    exception: Throwable
  ): Future[Result] =
    RequestMdc.initMdc(request.id)

    logger.error(
      """
        |
        |! %sInternal server error, for (%s) [%s] ->
        | """
        .stripMargin
        .format(
          exception match {
            case p: PlayException => "@" + p.id + " - "
            case _ => ""
          },
          request.method,
          request.uri
        ),
      exception
    )(using request)
    resolveError(request, exception)
