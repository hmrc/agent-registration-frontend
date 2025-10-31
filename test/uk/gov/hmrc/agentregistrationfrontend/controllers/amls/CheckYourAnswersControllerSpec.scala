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

package uk.gov.hmrc.agentregistrationfrontend.controllers.amls

import com.google.inject.AbstractModule
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  private val path = "/agent-registration/apply/anti-money-laundering/check-your-answers"

  "route should have correct path and method" in:
    routes.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  private case class TestCaseForCya(
    application: AgentApplication,
    amlsType: String,
    expectedRedirect: Option[String] = None
  )

  private val sectionAmls = tdAll.agentApplicationLlp.sectionAmls

  List(
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsHmrc
          .afterSupervisoryBodySelected,
      amlsType = "HMRC",
      expectedRedirect = Some(routes.AmlsRegistrationNumberController.show.url)
    ),
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsHmrc
          .afterRegistrationNumberProvided,
      amlsType = "HMRC",
      expectedRedirect = None
    ),
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsNonHmrc
          .afterUploadSucceded,
      amlsType = "non-HMRC",
      expectedRedirect = None
    ),
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsNonHmrc
          .afterSupervisoryBodySelected,
      amlsType = "non-HMRC",
      expectedRedirect = Some(routes.AmlsRegistrationNumberController.show.url)
    ),
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsNonHmrc
          .afterRegistrationNumberProvided,
      amlsType = "non-HMRC",
      expectedRedirect = Some(routes.AmlsExpiryDateController.show.url)
    ),
    TestCaseForCya(
      application =
        sectionAmls
          .whenSupervisorBodyIsNonHmrc
          .afterAmlsExpiryDateProvided,
      amlsType = "non-HMRC",
      expectedRedirect = Some(routes.AmlsEvidenceUploadController.show.url)
    )
  ).foreach: testCase =>
    if testCase.expectedRedirect.isEmpty then
      s"GET $path with complete amls details should return 200 and render page for ${testCase.amlsType}" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 200
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
    else
      s"GET $path with incomplete amls details should redirect to ${testCase.expectedRedirect.value} for ${testCase.amlsType}" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 303
        response.header("Location").value shouldBe testCase.expectedRedirect.value
