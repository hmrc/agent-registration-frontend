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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsSupervisorControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/supervisor-name"

  private object agentApplication:

    val baseForSectionAmls: AgentApplication = tdAll.agentApplicationLlpSections.baseForSectionAmls

    val afterSupervisoryBodySelected: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterSupervisoryBodySelected

    val afterHmrcSelected: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterSupervisoryBodySelected

    val afterHmrcRegistrationNumberStored: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

  private object ExpectedStrings:

    private val heading = "What is the name of the supervisory body for Test Company Name?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter a name and choose your supervisor from the list"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.AmlsSupervisorController.show shouldBe Call(
      method = "GET",
      url = "/agent-registration/apply/anti-money-laundering/supervisor-name"
    )
    AppRoutes.apply.amls.AmlsSupervisorController.submit shouldBe Call(
      method = "POST",
      url = "/agent-registration/apply/anti-money-laundering/supervisor-name"
    )
    AppRoutes.apply.amls.AmlsSupervisorController.submit.url shouldBe AppRoutes.apply.amls.AmlsSupervisorController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.baseForSectionAmls)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when supervisory body already stored should return 200 and render page with previous answer selected" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSupervisoryBodySelected)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    doc.select("select[name=amlsSupervisoryBody] option[selected]")
      .attr("value") shouldBe "ATT"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with valid selection should redirect to the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.baseForSectionAmls,
      updatedApplication = agentApplication.afterHmrcSelected
    )
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("HMRC")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdateWithBpr()

  s"POST $path when changing value should unset registration number and redirect to the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.afterHmrcRegistrationNumberStored,
      updatedApplication = agentApplication.afterSupervisoryBodySelected
    )
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("ATT")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdateWithBpr()

  s"POST $path with save for later and valid selection should redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.baseForSectionAmls,
      updatedApplication = agentApplication.afterHmrcSelected
    )
    val response: WSResponse =
      post(path)(Map(
        AmlsCodeForm.key -> Seq("HMRC"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdateWithBpr()

  s"POST $path without valid selection should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.baseForSectionAmls)
    val response: WSResponse =
      post(path)(
        Map(
          AmlsCodeForm.key -> Seq(Constants.EMPTY_STRING)
        )
      )

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${AmlsCodeForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
