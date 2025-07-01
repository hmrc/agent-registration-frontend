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

package uk.gov.hmrc.agentregistrationfrontend.views

import play.api.i18n.{Messages, MessagesProvider}
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.agentregistrationfrontend.config.ErrorHandler
import uk.gov.hmrc.agentregistrationfrontend.views.html.HelloWorldPage

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Views @Inject() (
    val helloWorldPage: HelloWorldPage,
    errorHandler: ErrorHandler,
)(implicit executionContext: ExecutionContext, messagesProvider: MessagesProvider) {

  def unauthorised(implicit request: RequestHeader): Future[Result] =
    errorHandler.standardErrorTemplate(
        pageTitle = Messages("unauthorised.title"),
        heading = Messages("unauthorised.heading"),
        message = "")
      .map(html => Unauthorized(html))
}


class ViewsTestOnly @Inject() (

)
