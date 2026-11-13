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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfPartnersResponsibleForTaxForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class NumberOfPartnersResponsibleForTaxControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/how-many-partners-responsible-for-tax"

  object agentApplication:

    val afterFiveOrLessChosen: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterSixOrMoreChosen: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterSixOrMoreChosen

    val afterSixOrMorePopulated: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterSixOrMorePopulated

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxController.submit.url shouldBe
      AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxController.show.url

  s"GET $path when SixOrMore chosen should return 200, fetch the BPR and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSixOrMoreChosen)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Partners who are also ‘relevant individuals’ - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when FiveOrLess chosen should redirect to the number of partners page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterFiveOrLessChosen)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with valid amount for number of partners who are also relevant should save data and redirect to CYA for working out the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.afterSixOrMoreChosen,
      updatedApplication = agentApplication.afterSixOrMorePopulated
    )
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("3")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSixOrMoreChosen)
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Partners who are also ‘relevant individuals’ - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax}-error"
    ).text() shouldBe "Error: Enter how many partners are also relevant individuals"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with selection of other and invalid characters should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSixOrMoreChosen)
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Partners who are also ‘relevant individuals’ - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax}-error"
    ).text() shouldBe "Error: Enter only numbers"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with a value higher than 30 should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSixOrMoreChosen)
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("31")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Partners who are also ‘relevant individuals’ - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax}-error"
    ).text() shouldBe "Error: Enter a number between 1 and 30"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.afterSixOrMoreChosen,
      updatedApplication = agentApplication.afterSixOrMorePopulated
    )
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("3"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterSixOrMoreChosen)
    val response: WSResponse =
      post(path)(Map(
        NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
