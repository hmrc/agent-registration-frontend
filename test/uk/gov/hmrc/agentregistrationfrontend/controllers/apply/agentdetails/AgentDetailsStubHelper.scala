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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

object AgentDetailsStubHelper:

  private val utr: Utr = Utr(tdAll.saUtr.value)

  def stubsForAuthAction(application: AgentApplicationLlp): StubMapping =
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)

  def stubsToRenderPage(application: AgentApplicationLlp): StubMapping =
    stubsForAuthAction(application)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse
    )

  def stubsForSuccessfulUpdate(
    application: AgentApplicationLlp,
    updatedApplication: AgentApplicationLlp
  ): StubMapping = {
    stubsForAuthAction(application)
    AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)
  }

  def verifyConnectorsForAuthAction(): Unit =
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  def verifyConnectorsToRenderPage(): Unit =
    verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)

  def verifyConnectorsForSuccessfulUpdate(): Unit =
    verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
