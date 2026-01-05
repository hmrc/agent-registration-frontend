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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.libs.ws.DefaultBodyReadables.*
import com.softwaremill.quicklens.modify
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

class MemberApproveApplicationControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/approve-applicant"

  object agentApplication:
    val applicationSubmitted: AgentApplicationLlp = tdAll
      .agentApplicationLlp
      .sectionAgentDetails
      .whenUsingExistingCompanyName
      .afterContactTelephoneSelected
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  private object memberProvideDetails:

    val afterSaUtrProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrProvided
    val withSaUtrNotProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.AfterNino.afterNinoProvided
    val withNinoAndSaUtrFromAuthButEmailNotProvided: MemberProvidedDetails = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrFromAuth.copy(
      emailAddress = None
    )
    val afterApproveAgentApplication = tdAll.providedDetailsLlp.afterApproveAgentApplication
    val afterDoNotApproveAgentApplication = tdAll.providedDetailsLlp.afterDoNotApproveAgentApplication

  private object ExpectedStrings:

    val heading = "Approve the applicant"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    def requiredError(applyOfficerName: String) = s"Error: Select yes if you agree that ${applyOfficerName} can apply for an account"

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.MemberApproveApplicantController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.MemberApproveApplicantController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.MemberApproveApplicantController.submit.url shouldBe AppRoutes.providedetails.MemberApproveApplicantController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.afterSaUtrProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title

    AuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId)

  s"GET $path should redirect to saUtr if is missing" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.withSaUtrNotProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.MemberSaUtrController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId, 1)

  s"GET $path should redirect to emailAddress if is missing" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.withNinoAndSaUtrFromAuthButEmailNotProvided))
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.MemberEmailAddressController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()
    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(tdAll.agentApplicationId, 1)

  s"POST $path with selected Yes should save data and redirect to agree standard" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.afterSaUtrProvided))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterApproveAgentApplication)
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)

    val response: WSResponse =
      post(path)(Map(
        MemberApproveApplicationForm.key -> Seq("Yes")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.MemberHmrcStandardForAgentsController.show.url

  s"POST $path with selected No should save data and redirect to agree standard" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.afterSaUtrProvided))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterDoNotApproveAgentApplication)
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)

    val response: WSResponse =
      post(path)(Map(
        MemberApproveApplicationForm.key -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.MemberConfirmStopController.show.url

  s"POST $path  without selecting and option should return 400" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvideDetails.afterSaUtrProvided))
    AgentRegistrationMemberProvidedDetailsStubs.stubUpsertMemberProvidedDetails(memberProvideDetails.afterApproveAgentApplication)
    AgentRegistrationStubs.stubFindApplication(tdAll.agentApplicationId, agentApplication.applicationSubmitted)

    val response: WSResponse =
      post(path)(Map(
        MemberApproveApplicationForm.key -> Seq().empty
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select("#memberApproveAgentApplication-error").text() shouldBe ExpectedStrings.requiredError(
      agentApplication.applicationSubmitted.getApplicantContactDetails.getApplicantName
    )
