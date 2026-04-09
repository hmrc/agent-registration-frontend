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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CitizenDetailsStub

class UnifiedCustomerRegistryControllerSpec
extends ControllerSpec:

  private val path: String = "/agent-registration/apply/internal/unified-customer-registry-identifiers"
  private val nextPageUrl: String = "/agent-registration/apply/task-list"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.UnifiedCustomerRegistryController.updateApplicationIdentifiers shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with vrns and payeRefs if present and redirect to task list page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetOrganisationIdentifiers(tdAll.saUtr.asUtr, tdAll.ucrIdentifiers)
    AgentRegistrationStubs.stubGetAgentApplication(tdAll.agentApplicationLlp.afterCompaniesHouseStatusCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(tdAll.agentApplicationLlp.afterUnifiedCustomerRegistryUpdateIdentifiers)
    CitizenDetailsStub.stubDesignatoryDetailsFound(nino = tdAll.nino)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CitizenDetailsStub.verifyDesignatoryDetails(nino = tdAll.nino)
