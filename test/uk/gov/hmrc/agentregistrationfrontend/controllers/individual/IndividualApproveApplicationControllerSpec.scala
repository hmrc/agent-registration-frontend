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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class IndividualApproveApplicationControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val path = s"/agent-registration/provide-details/approve-applicant/${linkId.value}"

  val applicationInProgress: AgentApplication =
    tdAll
      .agentApplicationLlp
      .sectionAgentDetails
      .whenUsingExistingCompanyName
      .afterContactTelephoneSelected

  private object individualProvideDetails:

    val afterSaUtrProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterSaUtr.afterSaUtrProvided
    val withSaUtrNotProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoProvided
    val withNinoAndSaUtrFromAuthButEmailNotProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterSaUtr.afterSaUtrFromAuth.copy(
      emailAddress = None
    )
    val afterApproveAgentApplication: IndividualProvidedDetails = tdAll.providedDetails.afterApproveAgentApplication
    val afterDoNotApproveAgentApplication: IndividualProvidedDetails = tdAll.providedDetails.afterDoNotApproveAgentApplication

  private object ExpectedStrings:

    val heading = "Approve the applicant"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    def requiredError(applyOfficerName: String) = s"Error: Select yes if you agree that ${applyOfficerName} can apply for an account"

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.IndividualApproveApplicantController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualApproveApplicantController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.IndividualApproveApplicantController.submit(
      linkId
    ).url shouldBe AppRoutes.providedetails.IndividualApproveApplicantController.show(linkId).url

  s"GET $path should return 200 and render page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      applicationInProgress,
      individualProvideDetails.afterSaUtrProvided
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe ExpectedStrings.title
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"GET $path should redirect to saUtr if is missing" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      applicationInProgress,
      individualProvideDetails.withSaUtrNotProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualSaUtrController.show(linkId).url
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"GET $path should redirect to emailAddress if is missing" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      applicationInProgress,
      individualProvideDetails.withNinoAndSaUtrFromAuthButEmailNotProvided
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with selected Yes should save data and redirect to agree standard" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = applicationInProgress,
      individualProvidedDetails = individualProvideDetails.afterApproveAgentApplication,
      updatedIndividualProvidedDetails = individualProvideDetails.afterApproveAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualApproveApplicationForm.key -> Seq("Yes")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show(linkId).url

  s"POST $path with selected No should save data and redirect to agree standard" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = applicationInProgress,
      individualProvidedDetails = individualProvideDetails.afterSaUtrProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterDoNotApproveAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualApproveApplicationForm.key -> Seq("No")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualConfirmStopController.show.url

  s"POST $path without selecting an option should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      applicationInProgress,
      individualProvideDetails.afterSaUtrProvided
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.asUtr,
      responseBody = tdAll.businessPartnerRecordResponse
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualApproveApplicationForm.key -> Seq().empty
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select("#individualApproveAgentApplication-error").text() shouldBe ExpectedStrings.requiredError(
      applicationInProgress.getApplicantContactDetails.applicantName.value
    )
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
