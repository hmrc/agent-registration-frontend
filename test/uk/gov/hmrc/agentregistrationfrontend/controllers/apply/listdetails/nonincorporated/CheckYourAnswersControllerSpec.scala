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

import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/key-individuals/check-your-answers"

  object agentApplication:

    val beforeHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHmrcStandardForAgentsAgreed

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

    val afterOnlyOneKeyIndividual: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterOnlyOneKeyIndividual

    // SixOrMore selected and less than 5 nominated
    val afterHowManyKeyIndividualsNeedsPadding: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividualsNeedsPadding

  final case class TestCase(
    description: String,
    agentApplication: AgentApplicationGeneralPartnership,
    existingIndividuals: List[IndividualProvidedDetails],
    expectedHeading: String,
    expectedButtonText: Option[String] = None,
    expectedInsetText: Option[String] = None,
    expectedWarningText: Option[String] = None
  )

  val testCases: List[TestCase] = List(
    TestCase(
      description = "list is complete with 3 individuals",
      agentApplication = agentApplication.afterHowManyKeyIndividuals,
      existingIndividuals = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      ),
      expectedHeading = "You have added 3 partners",
      expectedButtonText = Some("Confirm and continue Save and come back later") // there are 2 buttons expected
    ),
    TestCase(
      description = "list needs padding and is incomplete with 1 existing individual",
      agentApplication = agentApplication.afterHowManyKeyIndividualsNeedsPadding,
      existingIndividuals = List(tdAll.individualProvidedDetails),
      expectedHeading = "You have added 1 partner",
      expectedButtonText = Some("Add another partner Save and come back later"), // there are 2 buttons expected
      expectedInsetText = Some("We need the names of: the 3 partners responsible for tax advice 2 other partners")
    ),
    TestCase(
      description = "list is incomplete and has 1 existing individual",
      agentApplication = agentApplication.afterHowManyKeyIndividuals,
      existingIndividuals = List(tdAll.individualProvidedDetails),
      expectedHeading = "You have added 1 partner",
      expectedButtonText = Some("Add another partner Save and come back later"), // there are 2 buttons expected
      expectedInsetText = Some("You need to tell us about 2 more partners")
    ),
    TestCase(
      description = "list has too many individuals",
      agentApplication = agentApplication.afterOnlyOneKeyIndividual,
      existingIndividuals = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      ),
      expectedHeading = "You have added 3 partners",
      expectedWarningText = Some(
        "Warning You told us there is 1 partner. Change the number of partners or remove 2 partners from the list before you continue."
      )
    )
  )

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  testCases.foreach: testCase =>
    s"GET $path should return 200 and render page when ${testCase.description}" in:
      ApplyStubHelper.stubsForAuthAction(testCase.agentApplication)
      AgentRegistrationStubs.stubFindIndividualsForApplication(
        agentApplicationId = testCase.agentApplication.agentApplicationId,
        individuals = testCase.existingIndividuals
      )
      val response: WSResponse = get(path)

      response.status shouldBe Status.OK
      val doc: Document = response.parseBodyAsJsoupDocument
      doc.title() shouldBe s"${testCase.expectedHeading} - Apply for an agent services account - GOV.UK"
      doc.select("h1").text() shouldBe testCase.expectedHeading
      testCase.expectedButtonText.foreach: expectedButtonText =>
        doc.select("main a.govuk-button").text() shouldBe expectedButtonText
      testCase.expectedInsetText.foreach: expectedInsetText =>
        doc.select("main .govuk-inset-text").text() shouldBe expectedInsetText
      testCase.expectedWarningText.foreach: expectedWarningText =>
        doc.select("main .govuk-warning-text__text").text() shouldBe expectedWarningText
      ApplyStubHelper.verifyConnectorsForAuthAction()
      AgentRegistrationStubs.verifyFindIndividualsForApplication(testCase.agentApplication.agentApplicationId)

  s"GET $path when list is empty should redirect to enter a key individual page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterOnlyOneKeyIndividual)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List.empty
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHowManyKeyIndividuals.agentApplicationId)

  s"GET $path when no number of key individuals have been supplied should redirect to number of key individuals page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeHowManyKeyIndividuals)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header(HeaderNames.LOCATION) shouldBe Some(AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
