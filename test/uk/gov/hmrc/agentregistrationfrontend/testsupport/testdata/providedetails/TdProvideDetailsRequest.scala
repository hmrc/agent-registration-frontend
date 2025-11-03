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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.providedetails

import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.ProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.ProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdRequest

trait TdProvideDetailsRequest {
  dependencies: (TdBase & TdRequest) =>

  def makeProvideDetailsRequest(providedDetails: ProvidedDetails): ProvideDetailsRequest[AnyContent] =
    new ProvideDetailsRequest(
      request = dependencies.requestLoggedIn,
      providedDetails = providedDetails,
      internalUserId = dependencies.internalUserId,
      credentials = dependencies.credentials
    )

}
