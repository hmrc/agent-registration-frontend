/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Call, Result}
import uk.gov.hmrc.agentregistrationfrontend.model.application.Application
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnsureApplication @Inject() ()(implicit ec: ExecutionContext) extends RequestAwareLogging{

  /**
   * Check if application matches predicate. If it doesn't, it will send the Redirect.
   */
  def ensureApplication(predicate: Application => Boolean, redirectF: Application => Call, hintWhyRedirecting: String): ActionFilter[ApplicationRequest] = new ActionFilter[ApplicationRequest] {
    override def filter[A](request: ApplicationRequest[A]): Future[Option[Result]] = {
      implicit val r: ApplicationRequest[A] = request
      val application = request.application
      val result: Option[Result] =
        if (predicate(application)) None
        else {
          val call = redirectF(application)
          logger.warn(s"$hintWhyRedirecting (current application state: ${request.application.applicationState}), redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page.")
          Some(Redirect(call))
        }
      Future.successful(result)
    }

    override protected def executionContext: ExecutionContext = ec

  }
}
