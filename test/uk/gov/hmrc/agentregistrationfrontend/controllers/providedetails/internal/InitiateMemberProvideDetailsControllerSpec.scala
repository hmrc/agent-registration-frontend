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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.internal

import com.softwaremill.quicklens.modify
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.CitizenDetailsStub
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs
import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs

class InitiateMemberProvideDetailsControllerSpec
extends ControllerSpec:

  object agentApplication:

    val inComplete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val applicationSubmitted: AgentApplicationLlp = inComplete
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  object memberProvidedDetails:

    val afterStarted: MemberProvidedDetails =
      tdAll
        .providedDetailsLlp
        .afterStarted

    val afterStartedWithNinoAndSaUtrFromAuth: MemberProvidedDetails = tdAll
      .providedDetailsLlp
      .afterStarted
      .modify(_.memberNino)
      .setTo(Some(tdAll.ninoFromAuth))
      .modify(_.memberSaUtr)
      .setTo(Some(tdAll.saUtrFromAuth))

    val afterStartedWithNinoAndSaUtrFromCitizenDetails: MemberProvidedDetails = tdAll
      .providedDetailsLlp
      .afterStarted
      .modify(_.memberNino)
      .setTo(Some(tdAll.ninoFromAuth))
      .modify(_.memberSaUtr)
      .setTo(Some(tdAll.saUtrFromCitizenDetails))

  private def path(linkId: LinkId) = s"/agent-registration/provide-details/internal/initiate-member-provide-details/${linkId.value}"

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.internal.InitiateMemberProvideDetailsController.initiateMemberProvideDetails(tdAll.linkId) shouldBe Call(
      method = "GET",
      url = path(tdAll.linkId)
    )

  "GET initiateMemberProvideDetails should create memberProvidedDetails and redirect to member name page when application exists and memberProvidedDetails do not exist (nino only, saUtr from citizen details)" in:
    IndividualAuthStubs.stubAuthoriseWithNino()
    AgentRegistrationStubs.stubFindApplicationByLinkId(tdAll.linkId, agentApplication.applicationSubmitted)
    AgentRegistrationMemberProvidedDetailsStubs.stubFindMemberProvidedDetailsNoContent(agentApplication.applicationSubmitted.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvidedDetails.afterStartedWithNinoAndSaUtrFromCitizenDetails)
    CitizenDetailsStub.stubFindSaUtr(tdAll.nino, tdAll.saUtr)

    val response: WSResponse = get(path(tdAll.linkId))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url

    AgentRegistrationStubs.verifyFindApplicationByLinkId(tdAll.linkId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplication.applicationSubmitted.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyUpsert()
    CitizenDetailsStub.verifyFind(tdAll.nino)

  "GET initiateMemberProvideDetails should create memberProvidedDetails and redirect to member name page when application exists and memberProvidedDetails do not exist (nino only, saUtr from Enrolments)" in:
    IndividualAuthStubs.stubAuthoriseWithNinoAndSaUtr()
    AgentRegistrationStubs.stubFindApplicationByLinkId(tdAll.linkId, agentApplication.applicationSubmitted)
    AgentRegistrationMemberProvidedDetailsStubs.stubFindMemberProvidedDetailsNoContent(agentApplication.applicationSubmitted.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvidedDetails.afterStartedWithNinoAndSaUtrFromAuth)

    val response: WSResponse = get(path(tdAll.linkId))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url

    AgentRegistrationStubs.verifyFindApplicationByLinkId(tdAll.linkId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplication.applicationSubmitted.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyUpsert()
    CitizenDetailsStub.verifyFind(tdAll.nino, 0)

  "GET initiateMemberProvideDetails should not create new memberProvidedDetails and should redirect to member name page when memberProvidedDetails already exist" in:
    IndividualAuthStubs.stubAuthoriseWithNinoAndSaUtr()
    AgentRegistrationStubs.stubFindApplicationByLinkId(tdAll.linkId, agentApplication.applicationSubmitted)
    AgentRegistrationMemberProvidedDetailsStubs.stubFindMemberProvidedDetails(memberProvidedDetails.afterStartedWithNinoAndSaUtrFromAuth)

    val response: WSResponse = get(path(tdAll.linkId))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url

    AgentRegistrationStubs.verifyFindApplicationByLinkId(tdAll.linkId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplication.applicationSubmitted.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyUpsert(0)
    CitizenDetailsStub.verifyFind(tdAll.nino, 0)

  "GET initiateMemberProvideDetails should redirect to generic exit page when application is not found" in:
    IndividualAuthStubs.stubAuthoriseWithNinoAndSaUtr()
    AgentRegistrationStubs.stubFindApplicationByLinkIdNoContent(tdAll.linkId)

    val response: WSResponse = get(path(tdAll.linkId))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe
      AppRoutes.apply.AgentApplicationController.genericExitPage.url

    AgentRegistrationStubs.verifyFindApplicationByLinkId(tdAll.linkId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplication.applicationSubmitted.agentApplicationId, 0)
    AgentRegistrationMemberProvidedDetailsStubs.verifyUpsert(0)
    CitizenDetailsStub.verifyFind(tdAll.nino, 0)
