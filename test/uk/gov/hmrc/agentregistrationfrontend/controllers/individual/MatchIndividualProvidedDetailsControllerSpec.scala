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

class MatchIndividualProvidedDetailsControllerSpec
extends ControllerSpec:

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterContactDetailsComplete
  private val path = s"/agent-registration/provide-details/match-application/${linkId.value}"

  private object individualProvideDetails:

    val unclaimed: IndividualProvidedDetails = tdAll.providedDetails.unclaimed
    val afterStarted: IndividualProvidedDetails = tdAll.providedDetails.afterStarted

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.MatchIndividualProvidedDetailsController.submit(
      linkId
    ).url shouldBe AppRoutes.providedetails.MatchIndividualProvidedDetailsController.show(
      linkId
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
