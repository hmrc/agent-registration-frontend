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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.model.CompanyHouseStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData.crn
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CompaniesHouseStubs

class CompaniesHouseStatusCheckControllerSpec
extends ControllerSpec:

  object agentApplication:

    val beforeHmrcEntityVerificationPass: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

    val afterHmrcEntityVerificationPass =
      tdAll
        .agentApplicationLlp
        .afterHmrcEntityVerificationPass

    val afterCompaniesHouseStatusAllow =
      tdAll
        .agentApplicationLlp
        .afterCompaniesHouseStatusCheckAllow

    val afterCompaniesHouseStatusBlock =
      tdAll
        .agentApplicationLlp
        .afterCompaniesHouseStatusCheckBlock

  private val path: String = "/agent-registration/apply/internal/status-check"
  private val nextPageUrl: String = "/agent-registration/apply/task-list"
  private val previousPage: String = "/agent-registration/apply/internal/register-check"
  private val comapanyStatusBlock: String = "/agent-registration/apply/cannot-register-company-or-partnership"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.CompaniesHouseStatusController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with active company status and redirect to task list when company is active" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterHmrcEntityVerificationPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterCompaniesHouseStatusAllow)
    CompaniesHouseStubs.givenSuccessfulGetCompanyHouseResponse(crn = crn, companyStatus = CompanyHouseStatus.Active.key)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CompaniesHouseStubs.verifyGetCompanyHouse(crn = crn)

  s"GET $path should update application with fail status and open company status fail page when company status is blocked" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterHmrcEntityVerificationPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterCompaniesHouseStatusBlock)
    CompaniesHouseStubs.givenSuccessfulGetCompanyHouseResponse(crn = crn, companyStatus = CompanyHouseStatus.Closed.key)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe comapanyStatusBlock
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CompaniesHouseStubs.verifyGetCompanyHouse(crn = crn)

  s"GET $path should redirect to entity check  when entity checks not defined" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeHmrcEntityVerificationPass)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe previousPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $path should redirect to task list page when entity verification already done" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterCompaniesHouseStatusAllow)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextPageUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

//TODO - add test case for Sole trader
