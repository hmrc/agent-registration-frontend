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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class UnifiedCustomerRegistryControllerSpec
extends ControllerSpec:

  object agentApplication:

    val afterCompaniesHouseStatusCheckPass: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterCompaniesHouseStatusCheckPass

    val afterIdentifiersUpdated: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterUnifiedCustomerRegistryUpdateIdentifiers

    val afterEmptyIdentifiersUpdated: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterUnifiedCustomerRegistryUpdateEmptyIdentifiers

  private val path: String = "/agent-registration/apply/internal/unified-customer-registry-identifiers"
  private val nextPageUrl: String = "/agent-registration/apply/task-list"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.UnifiedCustomerRegistryController.populateApplicationIdentifiersFromUcr shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with vrns and payeRefs if present and redirect to task list page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetOrganisationIdentifiers(tdAll.saUtr.asUtr, tdAll.ucrIdentifiers)
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterCompaniesHouseStatusCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterIdentifiersUpdated)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetOrganisationIdentifiers(tdAll.saUtr.asUtr)
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should update application with empty list if vrns/ payeRefs not present and redirect to task list" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetOrganisationIdentifiers(tdAll.saUtr.asUtr, tdAll.emptyUcrIdentifiers)
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterCompaniesHouseStatusCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterEmptyIdentifiersUpdated)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetOrganisationIdentifiers(tdAll.saUtr.asUtr)
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should update application with empty lists if connector fails and redirect to task list" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetOrganisationIdentifiersFails(tdAll.saUtr.asUtr)
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterCompaniesHouseStatusCheckPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterEmptyIdentifiersUpdated)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetOrganisationIdentifiers(tdAll.saUtr.asUtr)
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should redirect to task list page when ucr identifiers already populated" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetOrganisationIdentifiers(tdAll.saUtr.asUtr, tdAll.ucrIdentifiers)
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterIdentifiersUpdated)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
