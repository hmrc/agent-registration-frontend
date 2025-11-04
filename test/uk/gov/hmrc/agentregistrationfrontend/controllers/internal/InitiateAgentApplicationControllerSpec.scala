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

package uk.gov.hmrc.agentregistrationfrontend.controllers.internal

import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.Partnership.LimitedLiabilityPartnership
import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
import uk.gov.hmrc.agentregistration.shared.util.SealedObjectsExtensions.toStringHyphenatedForSealedObjects
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class InitiateAgentApplicationControllerSpec
extends ControllerSpec:

  def initiateAgentApplication(
    agentType: AgentType,
    businessType: BusinessType
  ): String = s"/apply/internal/initiate-agent-application/${agentType.toStringHyphenated}/${businessType.toStringHyphenatedForSealedObjects}"

  final case class TestCase(
    agentType: AgentType,
    businessType: BusinessType
  )

  Seq(
    TestCase(AgentType.UkTaxAgent, LimitedLiabilityPartnership)
  ).foreach: t =>
    val url = initiateAgentApplication(agentType = t.agentType, businessType = t.businessType)
    s"routes should have correct paths and methods (${t.agentType}, ${t.businessType})" in:
      routes.InitiateAgentApplicationController.initiateAgentApplication(t.agentType, t.businessType) shouldBe Call(
        method = "GET",
        url = url
      )

    s"GET $url should create initial agent application" in:
      AuthStubs.stubAuthorise()
