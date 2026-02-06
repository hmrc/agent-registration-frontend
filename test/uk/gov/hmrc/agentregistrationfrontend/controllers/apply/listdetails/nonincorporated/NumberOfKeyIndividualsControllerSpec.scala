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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.nonincorporated

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfKeyIndividualsForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class NumberOfKeyIndividualsControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/how-many-key-individuals"

  object agentApplication:

    val beforeHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHmrcStandardForAgentsAgreed

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.submit.url shouldBe
      AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url

  s"GET $path should return 200, fetch the BPR and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterHowManyKeyIndividuals)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "How many partners are there at Test Company Name? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with valid amounts should save data and redirect to CYA for working out the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeHowManyKeyIndividuals,
      updatedApplication = agentApplication.afterHowManyKeyIndividuals
    )
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess"),
        NumberOfKeyIndividualsForm.howManyIndividuals -> Seq("3")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: How many partners are there at Test Company Name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfKeyIndividualsForm.howManyIndividualsOption}-error"
    ).text() shouldBe "Error: Select how many partners there are"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with selection of FiveOrLess and blank field for amount should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: How many partners are there at Test Company Name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfKeyIndividualsForm.howManyIndividuals}-error"
    ).text() shouldBe "Error: Enter a number between 1 and 5, for example 3"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with selection of other and invalid characters should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess"),
        NumberOfKeyIndividualsForm.howManyIndividuals -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: How many partners are there at Test Company Name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfKeyIndividualsForm.howManyIndividuals}-error"
    ).text() shouldBe "Error: Enter a number between 1 and 5, for example 3"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with selection of FiveOrLess and a value higher than 5 should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess"),
        NumberOfKeyIndividualsForm.howManyIndividuals -> Seq("25")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: How many partners are there at Test Company Name? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfKeyIndividualsForm.howManyIndividuals}-error"
    ).text() shouldBe "Error: Enter a number between 1 and 5, for example 3"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeHowManyKeyIndividuals,
      updatedApplication = agentApplication.afterHowManyKeyIndividuals
    )
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess"),
        NumberOfKeyIndividualsForm.howManyIndividuals -> Seq("3"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse =
      post(path)(Map(
        NumberOfKeyIndividualsForm.howManyIndividualsOption -> Seq("FiveOrLess"),
        NumberOfKeyIndividualsForm.howManyIndividuals -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
