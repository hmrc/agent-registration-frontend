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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmMatchToIndividualProvidedDetailsForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs

class MatchIndividualProvidedDetailsControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path = s"/agent-registration/provide-details/match-application/${linkId.value}"

  private object individualProvideDetails:

    val unclaimed: IndividualProvidedDetails = tdAll.providedDetails.precreated
    val afterStarted: IndividualProvidedDetails = tdAll.providedDetails.afterStarted
    val providedByApplicant: IndividualProvidedDetails = tdAll.providedDetails.AfterSaUtr.afterSaUtrProvided.copy(
      internalUserId = None,
      passedIv = Some(false),
      providedByApplicant = Some(true)
    )

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(linkId, fromIv = None) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.submit(linkId, fromIv = None) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.submit(
      linkId,
      fromIv = None
    ).url shouldBe AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(
      linkId,
      fromIv = None
    ).url

  s"GET $path should return 200 and render page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication,
      individualProvideDetails.unclaimed
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Confirm your details - Apply for an agent services account - GOV.UK"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"GET $path when user has low confidence level should redirect to Identity Verification uplift" in:
    IndividualAuthStubs.stubAuthorise(responseBody = IndividualAuthStubs.responseBodyAsCl50())
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe "http://localhost:9938/mdtp/uplift?origin=agent-registration-frontend&confidenceLevel=250&completionURL=http://localhost:19001/agent-registration/provide-details/match-application/link-id-12345?fromIv%3Dtrue&failureURL=http://localhost:19001/agent-registration/provide-details/match-application/link-id-12345?fromIv%3Dtrue"
    IndividualAuthStubs.verifyAuthorise()

  s"GET $path when CL50 user has failed IV uplift should redirect to manual name matching page" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication,
      individualProvideDetails.unclaimed,
      isScr = true
    )
    val response: WSResponse = get(path + "?fromIv=true")
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.NameMatchingController.show(linkId).url
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"GET $path when details already provided by applicant should redirect to dedicated exit page" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication,
      individualProvideDetails.providedByApplicant
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.ExitController.detailsAlreadyProvided.url
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with a valid choice should save data and redirect to CYA controller" in:
    ProvideDetailsStubHelper.stubAuthAndClaimMatchedIndividualProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.unclaimed,
      updatedIndividualProvidedDetails = individualProvideDetails.afterStarted
    )
    val response: WSResponse =
      post(path)(Map(
        ConfirmMatchToIndividualProvidedDetailsForm.key -> Seq("Yes")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with blank inputs should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication,
      individualProvideDetails.unclaimed
    )
    val response: WSResponse =
      post(path)(Map(
        ConfirmMatchToIndividualProvidedDetailsForm.key -> Seq("")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Confirm your details - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${ConfirmMatchToIndividualProvidedDetailsForm.key}-error").text() shouldBe "Error: Select yes if these details are correct"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()

  s"POST $path with invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
      agentApplication,
      individualProvideDetails.unclaimed
    )
    val response: WSResponse =
      post(path)(Map(
        ConfirmMatchToIndividualProvidedDetailsForm.key -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Confirm your details - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${ConfirmMatchToIndividualProvidedDetailsForm.key}-error").text() shouldBe "Error: Select yes if these details are correct"
    ProvideDetailsStubHelper.verifyAuthAndFindApplicationAndProvidedDetails()
