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

package uk.gov.hmrc.agentregistrationfrontend.testonly.action

import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedActionRefiner
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithAuth

import scala.concurrent.Future

@Singleton
final class TestOnlyAuthorisedAction @Inject() (
  authorisedActionRefiner: AuthorisedActionRefiner
):

  /** Public hook for test-only controllers. */
  def refinePublic(request: Request[AnyContent]): Future[Either[Result, RequestWithAuth]] =
    val requestWithEmptyData = RequestWithDataCt.empty(request)
    authorisedActionRefiner.refine(requestWithEmptyData)
