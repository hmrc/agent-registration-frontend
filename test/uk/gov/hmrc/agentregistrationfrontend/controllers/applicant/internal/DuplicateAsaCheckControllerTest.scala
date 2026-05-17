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
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.EnrolmentStoreStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.TaxEnrolmentsStubs

class DuplicateAsaCheckControllerTest
extends ControllerSpec:

  object agentApplication:

    val afterUnifiedCustomerRegistryUpdateIdentifiers: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterUnifiedCustomerRegistryUpdateIdentifiers

    val afterIsDuplicateAsaFalse: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterIsDuplicateAsaFalse

    val afterIsDuplicateAsaTrue: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterIsDuplicateAsaTrue

  private val arn: Arn = tdAll.arn
  private val utr: Utr = agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers.getUtr
  private val path: String = "/agent-registration/apply/internal/duplicate-asa-check"
  private val nextUrl: String = "/agent-registration/apply/task-list"
  private val alreadySubscribedPage: String = "/agent-registration/apply/already-subscribed"
  private val asaDashboard: String = "http://localhost:9401/agent-services-account/home"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.internal.DuplicateAsaCheckController.check() shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path should redirect to unified customer registry when business is not already registered as an agent" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterIsDuplicateAsaFalse)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)
    EnrolmentStoreStubs.verifyQueryArnHasPrincipalGroups(arn, 0)
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should set asa duplicate status true and redirect to already subscribed page when principal group ids found" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse.copy(
        agentReferenceNumber = Some(arn),
        isAnASAgent = true
      )
    )
    EnrolmentStoreStubs.stubQueryArnHasPrincipalGroups(arn)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterIsDuplicateAsaTrue)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe alreadySubscribedPage
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)
    EnrolmentStoreStubs.verifyQueryArnHasPrincipalGroups(arn)
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should set asa duplicate status true, update known facts and enrolments and redirect to asa dashboard page when ARN has no found principal groups" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUnifiedCustomerRegistryUpdateIdentifiers)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = utr,
      responseBody = tdAll.businessPartnerRecordResponse.copy(
        agentReferenceNumber = Some(arn),
        isAnASAgent = true
      )
    )
    TaxEnrolmentsStubs.stubAddKnownFacts(arn)
    TaxEnrolmentsStubs.stubEnrol(arn)
    EnrolmentStoreStubs.stubQueryArnHasNoPrincipalGroups(arn)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterIsDuplicateAsaTrue)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe asaDashboard
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr)
    TaxEnrolmentsStubs.verifyAddKnownFacts(arn)
    TaxEnrolmentsStubs.verifyEnrol(arn)
    EnrolmentStoreStubs.verifyQueryArnHasPrincipalGroups(arn)
    AgentRegistrationStubs.verifyUpdateAgentApplication()

  s"GET $path should redirect to unified customer registry when duplicate ASA check has already been done" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterIsDuplicateAsaFalse)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe nextUrl
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(utr, 0)
    EnrolmentStoreStubs.verifyQueryArnHasPrincipalGroups(arn, 0)
    AgentRegistrationStubs.verifyUpdateAgentApplication(0)
