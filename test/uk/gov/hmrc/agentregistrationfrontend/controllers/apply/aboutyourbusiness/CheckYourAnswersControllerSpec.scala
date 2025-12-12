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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/check-your-answers"

  "route should have correct path and method" in:
    routes.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  object agentApplication:

    val beforeGrsDataReceived: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterStarted

    val afterGrsDataReceived: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterGrsDataReceived

  private final case class TestCaseForCya(
    application: AgentApplicationLlp,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      application = agentApplication.afterGrsDataReceived
    ),
    TestCaseForCya(
      application = agentApplication.beforeGrsDataReceived,
      expectedRedirect = Some(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url)
    )
  ).foreach: testCase =>
    testCase.expectedRedirect match
      case None =>
        s"GET $path with complete GRS details should return 200 and render page" in:
          ApplyStubHelper.stubsForAuthAction(testCase.application)
          val response: WSResponse = get(path)

          response.status shouldBe Status.OK
          val doc = response.parseBodyAsJsoupDocument
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
          doc.select("h2.govuk-caption-l").text() shouldBe "About your business"
          ApplyStubHelper.verifyConnectorsForAuthAction()

      case Some(expectedRedirect) =>
        s"GET $path with missing GRS data should redirect to the agent type page" in:
          ApplyStubHelper.stubsForAuthAction(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe Status.SEE_OTHER
        response.body[String] shouldBe Constants.EMPTY_STRING
        response.header("Location").value shouldBe expectedRedirect
        ApplyStubHelper.verifyConnectorsForAuthAction()
