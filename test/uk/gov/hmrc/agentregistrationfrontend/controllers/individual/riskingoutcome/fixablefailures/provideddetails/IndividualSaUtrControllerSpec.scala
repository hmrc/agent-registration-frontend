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
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualSaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class IndividualSaUtrControllerSpec
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

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/self-assessment-unique-taxpayer-reference/${linkId.value}"

  private object individualProvideDetails:

    val afterSaUtrProvided: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetails
    val afterSaUtrNotProvided: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetailsWithoutIds

  "routes should have correct paths and methods" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.submit(
      linkId
    ).url shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.show(linkId).url

  s"GET $path should return 200 and render page when SaUtr is not provided in HMRC systems" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterSaUtrNotProvided,
      withBpr = true
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"

  s"POST $path with selected Yes and valid saUtr should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterSaUtrNotProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterSaUtrProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq(tdAll.saUtrProvided.saUtr.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(
      "Location"
    ).value shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with selected No should save data and redirect to check your answers" in:
    ProvideDetailsStubHelper.stubAuthAndUpdateProvidedDetails(
      agentApplication = agentApplication,
      individualProvidedDetails = individualProvideDetails.afterSaUtrProvided,
      updatedIndividualProvidedDetails = individualProvideDetails.afterSaUtrNotProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("No")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(
      "Location"
    ).value shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId).url

  s"POST $path with selected Yes and blank inputs should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterSaUtrNotProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST

    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualSaUtr\\.saUtr-error").text() shouldBe "Error: Enter your Self Assessment Unique Taxpayer Reference"

  s"POST $path with selected Yes and invalid characters should return 400" in:
    ProvideDetailsStubHelper.stubAuthAndFindApplicationAndProvidedDetails(
      agentApplication = agentApplication,
      individualProvideDetails = individualProvideDetails.afterSaUtrNotProvided,
      withBpr = true
    )
    val response: WSResponse =
      post(path)(Map(
        IndividualSaUtrForm.hasSaUtrKey -> Seq("Yes"),
        IndividualSaUtrForm.saUtrKey -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you have a Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("#individualSaUtr\\.saUtr-error").text() shouldBe "Error: Enter a Self Assessment Unique Taxpayer Reference in the correct format"
