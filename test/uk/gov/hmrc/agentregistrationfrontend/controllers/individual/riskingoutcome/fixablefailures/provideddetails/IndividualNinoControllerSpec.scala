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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures.provideddetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNinoForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class IndividualNinoControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val linkId = tdAll.linkId
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterRiskingCompletedFixable
  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/national-insurance-number/${linkId.value}"

  private object individualProvideDetails:

    val afterNinoProvided: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetails
    val afterNinoNotProvided: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetailsWithoutIds

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.submit(
      linkId
    ).url shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.show(linkId).url

  s"GET $path should return 200 and render page when Nino is already provided in HMRC systems" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterNinoProvided,
      withBpr = true
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"

  s"GET $path should return 200 and render page when Nino is not provided in HMRC systems" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterNinoNotProvided,
      withBpr = true
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"

  s"POST $path with selected Yes and valid nino should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterNinoProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterNinoProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq(tdAll.ninoProvided.nino.value)
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(
      "Location"
    ).value shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with selected No should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubFixableFailureUpdate(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterNinoProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterNinoNotProvided
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("No")
      ))
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(
      "Location"
    ).value shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with selected Yes and blank inputs should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterNinoNotProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualNino\\.nino-error").text() shouldBe "Error: Enter your National Insurance number"

  s"POST $path with selected Yes and invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterNinoNotProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualNinoForm.hasNinoKey -> Seq("Yes"),
        IndividualNinoForm.ninoKey -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a National Insurance number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualNino\\.nino-error").text() shouldBe "Error: Enter a National Insurance number in the correct format"
