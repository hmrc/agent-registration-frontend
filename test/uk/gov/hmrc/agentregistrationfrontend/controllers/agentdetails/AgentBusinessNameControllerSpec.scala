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

package uk.gov.hmrc.agentregistrationfrontend.controllers.agentdetails

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.DesBusinessAddress
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentBusinessNameForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

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

  val bprResponse: BusinessPartnerRecordResponse = BusinessPartnerRecordResponse(
    organisationName = Some("Test Company Name"),
    address = DesBusinessAddress(
      addressLine1 = "Line 1",
      addressLine2 = Some("Line 2"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("AB1 2CD"),
      countryCode = "GB"
    ),
    emailAddress = Some(tdAll.applicantEmailAddress.value),
    primaryPhoneNumber = Some(tdAll.telephoneNumber.value)
  )

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
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"

  s"GET $path when existing name already chosen should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    val radioForExistingBusinessName = doc.mainContent.select(s"input#${AgentBusinessNameForm.key}")
    radioForExistingBusinessName.attr("value") shouldBe "Test Company Name"
    radioForExistingBusinessName.attr("checked") shouldBe "" // checked attribute is present when selected and has no value

  s"GET $path when new name already provided should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterNewBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"input#${AgentBusinessNameForm.otherKey}")
      .attr("value") shouldBe "New Agent Business Llp"

  s"POST $path with selection of existing company name should save data and redirect to CYA page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterBusinessNameReused)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("Test Company Name")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with selection of other and valid input for other name should save data and redirect to CYA page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterNewBusinessNameProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("New Agent Business Llp")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
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

  s"POST $path with selection of other and blank field for other name should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
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

  s"POST $path with selection of other and invalid characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
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

  s"POST $path with selection of other and more than 40 characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("A".repeat(41))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What business name will you use for clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${AgentBusinessNameForm.otherKey}-error").text() shouldBe "Error: Name shown to clients must be 40 characters or less"

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterBusinessNameReused)
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("Test Company Name"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = tdAll.saUtr.value,
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentBusinessNameForm.key -> Seq("other"),
        AgentBusinessNameForm.otherKey -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe applicationRoutes.SaveForLaterController.show.url
