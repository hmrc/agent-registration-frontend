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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentBusinessNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AgentBusinessNameControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-details/business-name"

  object agentApplication:

    val beforeBusinessNameProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterContactDetailsComplete

    val afterBusinessNameReused: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBusinessNameProvided

    val afterNewBusinessNameProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenProvidingNewBusinessName
        .afterBusinessNameProvided

  "routes should have correct paths and methods" in:
    routes.AgentBusinessNameController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AgentBusinessNameController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AgentBusinessNameController.submit.url shouldBe routes.AgentBusinessNameController.show.url

  s"GET $path should return 200 and render page" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"GET $path when existing name already chosen should return 200 and render page with previous answer filled in" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterBusinessNameReused)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    val radioForExistingBusinessName = doc.mainContent.select(s"input#${AgentBusinessNameForm.key}")
    radioForExistingBusinessName.attr("value") shouldBe "Test Company Name"
    radioForExistingBusinessName.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"GET $path when new name already provided should return 200 and render page with previous answer filled in" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.afterNewBusinessNameProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"input#${AgentBusinessNameForm.otherKey}")
      .attr("value") shouldBe "New Agent Business Llp"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with selection of existing company name should save data and redirect to CYA page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeBusinessNameProvided,
      updatedApplication = agentApplication.afterBusinessNameReused
    )
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("Test Company Name")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with selection of other and valid input for other name should save data and redirect to CYA page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeBusinessNameProvided,
      updatedApplication = agentApplication.afterNewBusinessNameProvided
    )
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("New Agent Business Llp")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with blank inputs should return 400" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentBusinessNameForm.key}-error"
    ).text() shouldBe "Error: Enter the business name you want to use on your agent services account"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with selection of other and blank field for other name should return 400" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentBusinessNameForm.otherKey}-error"
    ).text() shouldBe "Error: Enter the business name you want to use on your agent services account"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with selection of other and invalid characters should return 400" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentBusinessNameForm.otherKey}-error"
    ).text() shouldBe "Error: Name shown to clients must only include letters a to z, numbers, commas, full stops, apostrophes, hyphens, forward slashes and spaces"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with selection of other and more than 40 characters should return 400" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("A".repeat(41))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${AgentBusinessNameForm.otherKey}-error").text() shouldBe "Error: Name shown to clients must be 40 characters or less"
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    ApplyStubHelper.stubsForSuccessfulUpdate(
      application = agentApplication.beforeBusinessNameProvided,
      updatedApplication = agentApplication.afterBusinessNameReused
    )
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("Test Company Name"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AgentDetailsStubHelper.stubsToRenderPage(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    AgentDetailsStubHelper.verifyConnectorsToRenderPage()
