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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsExpiryDateForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsExpiryDateControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/supervision-runs-out"

  private object agentApplication:

    val hmrcAmls: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

    val beforeRegistrationNumberProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterSupervisoryBodySelected

    val afterRegistrationNumberProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterRegistrationNumberProvided

    val afterAmlsExpiryDateProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

  private object ExpectedStrings:

    val heading = "When does your supervision run out?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter your supervision expiry date"
    val invalidError = "Enter a valid date"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.AmlsExpiryDateController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.amls.AmlsExpiryDateController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.amls.AmlsExpiryDateController.submit.url shouldBe AppRoutes.apply.amls.AmlsExpiryDateController.show.url

  s"GET $path should return 200 render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when expiry date already stored should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterAmlsExpiryDateProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title shouldBe ExpectedStrings.title
    doc.select(s"input[name='${AmlsExpiryDateForm.dayKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getDayOfMonth.toString
    doc.select(s"input[name='${AmlsExpiryDateForm.monthKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getMonthValue.toString
    doc.select(s"input[name='${AmlsExpiryDateForm.yearKey}']")
      .attr("value") shouldBe agentApplication.afterAmlsExpiryDateProvided
      .getAmlsDetails
      .getAmlsExpiryDate
      .getYear.toString
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when registration number is missing should redirect to registration number page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeRegistrationNumberProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.AmlsRegistrationNumberController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when supervisor is HMRC should redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.hmrcAmls)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with valid inputs should redirect to the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterRegistrationNumberProvided,
      updatedApplication = agentApplication.afterAmlsExpiryDateProvided
    )
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateValid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateValid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateValid.getYear.toString),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and valid input should redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.afterRegistrationNumberProvided,
      updatedApplication = agentApplication.afterAmlsExpiryDateProvided
    )
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateValid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateValid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateValid.getYear.toString),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path as blank form should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(""),
        AmlsExpiryDateForm.monthKey -> Seq(""),
        AmlsExpiryDateForm.yearKey -> Seq(""),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.errorTitle
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path as blank form and save for later should redirect to save for later page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(""),
        AmlsExpiryDateForm.monthKey -> Seq(""),
        AmlsExpiryDateForm.yearKey -> Seq(""),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with an invalid value should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateInvalid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateInvalid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateInvalid.getYear.toString),
        "submit" -> Seq("SaveAndContinue")
      ))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.errorTitle
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with an invalid value and save for later should redirect to save for later" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterRegistrationNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AmlsExpiryDateForm.dayKey -> Seq(tdAll.amlsExpiryDateInvalid.getDayOfMonth.toString),
        AmlsExpiryDateForm.monthKey -> Seq(tdAll.amlsExpiryDateInvalid.getMonthValue.toString),
        AmlsExpiryDateForm.yearKey -> Seq(tdAll.amlsExpiryDateInvalid.getYear.toString),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
