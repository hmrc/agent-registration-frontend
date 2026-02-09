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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.CheckResult
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
        .afterRefusalToDealWithCheckPass

    val afterCompaniesHouseStatusCheckPass =
      tdAll
        .agentApplicationLlp
        .afterCompaniesHouseStatusCheckPass

    val afterCompaniesHouseStatusCheckFail =
      tdAll
        .agentApplicationLlp
        .afterCompaniesHouseStatusCheckFail

    val afterDeceasedCheckPassSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .afterDeceasedCheckPass

  private val path: String = "/agent-registration/apply/internal/companies-house-status-check"
  private val nextUrl: String = "/agent-registration/apply/task-list"
  private val failCheckPage: String = "/agent-registration/apply/cannot-register-company-or-partnership"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.CompaniesHouseStatusController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should update application with pass company status check and redirect to task list when company is active" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterHmrcEntityVerificationPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterCompaniesHouseStatusCheckPass)
    CompaniesHouseStubs.givenSuccessfulGetCompanyHouseResponse(crn = crn, companyStatus = CompanyHouseStatus.Active.key)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CompaniesHouseStubs.verifyGetCompanyHouse(crn = crn)

  s"GET $path should update application with fail status and open company status fail page when company status is blocked" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterHmrcEntityVerificationPass)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterCompaniesHouseStatusCheckFail)
    CompaniesHouseStubs.givenSuccessfulGetCompanyHouseResponse(crn = crn, companyStatus = CompanyHouseStatus.Closed.key)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe failCheckPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CompaniesHouseStubs.verifyGetCompanyHouse(crn = crn)

  s"GET path should redirect to task list page when entity verification already done" in:
    AuthStubs.stubAuthorise()
    val aa: AgentApplicationLlp = agentApplication.afterCompaniesHouseStatusCheckPass
    aa.companyStatusCheckResult shouldBe Some(CheckResult.Pass)
    println(Json.prettyPrint(Json.toJson(aa: AgentApplication)))
    AgentRegistrationStubs.stubGetAgentApplication(aa)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)

  s"GET $path should run company status check when comapny status check Fail" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterCompaniesHouseStatusCheckFail)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterCompaniesHouseStatusCheckPass)
    CompaniesHouseStubs.givenSuccessfulGetCompanyHouseResponse(crn = crn, companyStatus = CompanyHouseStatus.Active.key)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication()
    CompaniesHouseStubs.verifyGetCompanyHouse(crn = crn)

  s"GET $path should redirect to task list page when business type is not incorporated like SoleTrader" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterDeceasedCheckPassSoleTrader)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
