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

import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.SealedObjectsExtensions.toStringHyphenated
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as appRoutes
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec

class GrsConfigSpec
extends ISpec:

  val grsConfig: GrsConfig = app.injector.instanceOf[GrsConfig]

  "GrsConfig.grsJourneyCallbackUrl should point to valid endpoint defined in our routs file" should:
    BusinessType.values.foreach: (businessType: BusinessType) =>
      businessType.toString in:

        val thisFrontendBaseUrl: String = "http://localhost:22201"
        val expectedPath: String = s"/agent-registration/apply/grs-callback/${businessType.toStringHyphenated}"
        val withJourneyIdQuery: String = "?journeyId=journey-id-added-by-grs"

        grsConfig.grsJourneyCallbackUrl(businessType) shouldBe s"$thisFrontendBaseUrl$expectedPath"
        appRoutes.GrsController.journeyCallback(JourneyId("journey-id-added-by-grs")).url shouldBe s"$expectedPath$withJourneyIdQuery"
