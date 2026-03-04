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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.ConfirmNameMatchForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class NameMatchConfirmationControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val testName = "Test Name"

  private val path = s"/agent-registration/provide-details/name-match-confirmation/${linkId.value}"

  val completeAgentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .sectionContactDetails
      .afterEmailAddressVerified

  val listOfAgentProvidedDetails: List[IndividualProvidedDetails] = List(
    individualProvidedDetails,
    individualProvidedDetails2,
    individualProvidedDetails3
  )

  object testIndividualProvidedDetails:
    val unclaimedDetails: IndividualProvidedDetails =
      tdAll
        .providedDetails
        .unclaimed

  "NameMatchConfirmationController should have the correct routes" in:
    AppRoutes.providedetails.NameMatchConfrimationController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.NameMatchConfrimationController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.NameMatchConfrimationController.submit(linkId).url shouldBe
      AppRoutes.providedetails.NameMatchConfrimationController.show(linkId).url

  s"GET $path should return 200 and render the name confirmation page with the matched name" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      completeAgentApplication,
      testIndividualProvidedDetails.unclaimedDetails
    )

    val response: WSResponse = get(
      uri = path,
      cookies = addIndividualNameToSession(individualName = testName).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are these details correct? - Apply for an agent services account - GOV.UK"
    response.parseBodyAsJsoupDocument.select("dl.govuk-summary-list").text() should include("Test Name")

  "GET $path should redirect to exit page when agent application is missing" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkIdNoContent(linkId)
    val response = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.ExitController.genericExitPage.url

  s"POST $path should return 200 redirect to the check your answers page when the user agrees with the match" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      completeAgentApplication,
      testIndividualProvidedDetails.unclaimedDetails
    )

    val response: WSResponse =
      post(
        uri = path,
        cookies = addIndividualNameToSession(individualName = testName).extractCookies
      )(Map(ConfirmNameMatchForm.key -> Seq(YesNo.Yes.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url

  s"POST $path should return 200 redirect to enter your name page when the user does not agree with the match" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      completeAgentApplication,
      testIndividualProvidedDetails.unclaimedDetails
    )
    val response: WSResponse =
      post(
        uri = path,
        cookies = addIndividualNameToSession(individualName = testName).extractCookies
      )(Map(ConfirmNameMatchForm.key -> Seq(YesNo.No.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.providedetails.NameMatchingController.show(linkId).url

  s"POST $path should return 400 bad request when the user attempts to continue without an answer" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      completeAgentApplication,
      testIndividualProvidedDetails.unclaimedDetails
    )

    val response: WSResponse =
      post(
        uri = path,
        cookies = addIndividualNameToSession(individualName = testName).extractCookies
      )(Map())

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Are these details correct? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${ConfirmNameMatchForm.key}-error").text() shouldBe "Error: Confirm whether or not these details are correct"
