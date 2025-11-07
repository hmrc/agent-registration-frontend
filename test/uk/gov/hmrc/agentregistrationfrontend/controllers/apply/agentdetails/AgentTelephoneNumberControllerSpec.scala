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
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.DesBusinessAddress
import uk.gov.hmrc.agentregistration.shared.Utr
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AgentTelephoneNumberControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-details/telephone-number"

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

    val afterBprTelephoneNumberSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprTelephoneNumberSelected

    val afterContactTelephoneSelected: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val afterOtherTelephoneNumberProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterOtherTelephoneNumberProvided

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
    primaryPhoneNumber = Some(tdAll.bprPrimaryTelephoneNumber)
  )

  "routes should have correct paths and methods" in:
    routes.AgentTelephoneNumberController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AgentTelephoneNumberController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.AgentTelephoneNumberController.submit.url shouldBe routes.AgentTelephoneNumberController.show.url

  s"GET $path before business name has been selected should redirect to the business name page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeBusinessNameProvided)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AgentBusinessNameController.show.url

  s"GET $path should return 200, fetch the BPR and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"GET $path when existing contact telephone number already chosen should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBprTelephoneNumberSelected)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    val radioForContactTelephoneNumber = doc.mainContent.select(s"input#${AgentTelephoneNumberForm.key}")
    radioForContactTelephoneNumber.attr("value") shouldBe tdAll.telephoneNumber.value
    radioForContactTelephoneNumber.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"GET $path when existing BPR telephone number already chosen should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBprTelephoneNumberSelected)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    val radioForBprTelephoneNumber = doc.mainContent.select(s"input#${AgentTelephoneNumberForm.key}-2")
    radioForBprTelephoneNumber.attr("value") shouldBe tdAll.bprPrimaryTelephoneNumber
    radioForBprTelephoneNumber.attr("checked") shouldBe "" // checked attribute is present when selected and has no value
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"GET $path when new telephone number already provided should return 200 and render page with previous answer filled in" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterOtherTelephoneNumberProvided)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"input#${AgentTelephoneNumberForm.otherKey}")
      .attr("value") shouldBe tdAll.newTelephoneNumber
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"POST $path with selection of existing telephone number should save data and redirect to CYA page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterBprTelephoneNumberSelected)
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq(tdAll.bprPrimaryTelephoneNumber)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with selection of other and valid input for other name should save data and redirect to CYA page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterOtherTelephoneNumberProvided)
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("other"),
        AgentTelephoneNumberForm.otherKey -> Seq("+44 (0) 7000000000")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"POST $path with blank inputs should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentTelephoneNumberForm.key}-error"
    ).text() shouldBe "Error: Enter the telephone number for your agent services account"
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"POST $path with selection of other and blank field for other telephone number should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("other"),
        AgentTelephoneNumberForm.otherKey -> Seq("")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentTelephoneNumberForm.otherKey}-error"
    ).text() shouldBe "Error: Enter the telephone number for your agent services account"
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"POST $path with selection of other and invalid characters should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("other"),
        AgentTelephoneNumberForm.otherKey -> Seq("[[)(*%")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${AgentTelephoneNumberForm.otherKey}-error"
    ).text() shouldBe "Error: Telephone number must only include numbers, plus sign, hash sign, hyphens, brackets and spaces"
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"POST $path with selection of other and more than 24 characters without spaces should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("other"),
        AgentTelephoneNumberForm.otherKey -> Seq("A".repeat(25))
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What telephone number should we use for your agent services account? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${AgentTelephoneNumberForm.otherKey}-error").text() shouldBe "Error: The phone number must be 24 characters or fewer"
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))

  s"POST $path with save for later and valid selection should save data and redirect to the saved for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterBprTelephoneNumberSelected)
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq(tdAll.bprPrimaryTelephoneNumber),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url

  s"POST $path with save for later and invalid inputs should not return errors and redirect to save for later page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterBusinessNameReused)
    AgentRegistrationStubs.stubGetBusinessPartnerRecord(
      utr = Utr(tdAll.saUtr.value),
      responseBody = bprResponse
    )
    val response: WSResponse =
      post(path)(Map(
        AgentTelephoneNumberForm.key -> Seq("other"),
        AgentTelephoneNumberForm.otherKey -> Seq("[[)(*%"),
        "submit" -> Seq("SaveAndComeBackLater")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.SaveForLaterController.show.url
    AgentRegistrationStubs.verifyGetBusinessPartnerRecord(Utr(tdAll.saUtr.value))
