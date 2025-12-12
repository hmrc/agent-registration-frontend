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

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/agent-details/check-your-answers"

  "route should have correct path and method" in:
    routes.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  object agentApplication:

    val complete: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprAddressSelected

    val missingVerifiedEmail: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterContactTelephoneSelected

    val missingAddress: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterVerifiedEmailAddressSelected

    val missingEmail: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBprTelephoneNumberSelected

    val missingTelephone: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAgentDetails
        .whenUsingExistingCompanyName
        .afterBusinessNameProvided

    val missingBusinessName: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterEmailAddressVerified

  private final case class TestCaseForCya(
    application: AgentApplicationLlp,
    name: String,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      application = agentApplication.complete,
      name = "complete agent details"
    ),
    TestCaseForCya(
      application = agentApplication.missingAddress,
      name = "correspondence address",
      expectedRedirect = Some(routes.AgentCorrespondenceAddressController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingVerifiedEmail,
      name = "verified email address",
      expectedRedirect = Some(routes.AgentEmailAddressController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingEmail,
      name = "email address",
      expectedRedirect = Some(routes.AgentEmailAddressController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingTelephone,
      name = "telephone number",
      expectedRedirect = Some(routes.AgentTelephoneNumberController.show.url)
    ),
    TestCaseForCya(
      application = agentApplication.missingBusinessName,
      name = "business name",
      expectedRedirect = Some(routes.AgentBusinessNameController.show.url)
    )
  ).foreach: testCase =>
    testCase.expectedRedirect match
      case None =>
        s"GET $path with ${testCase.name} should return 200 and render page" in:
          ApplyStubHelper.stubsForAuthAction(testCase.application)
          val response: WSResponse = get(path)

          response.status shouldBe Status.OK
          val doc = response.parseBodyAsJsoupDocument
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
          doc.select("h2.govuk-caption-l").text() shouldBe "Agent services account details"
          ApplyStubHelper.verifyConnectorsForAuthAction()
      case Some(expectedRedirect) =>
        s"GET $path with missing ${testCase.name} should redirect to the ${testCase.name} page" in:
          ApplyStubHelper.stubsForAuthAction(testCase.application)
          val response: WSResponse = get(path)

          response.status shouldBe Status.SEE_OTHER
          response.header("Location").value shouldBe expectedRedirect
          ApplyStubHelper.verifyConnectorsForAuthAction()
