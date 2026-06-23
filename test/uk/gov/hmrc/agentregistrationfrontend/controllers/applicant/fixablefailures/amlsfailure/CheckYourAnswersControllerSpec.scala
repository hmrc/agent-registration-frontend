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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import com.google.inject.AbstractModule
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  private val path = "/agent-registration/conditions-not-yet-met/anti-money-laundering/check-your-answers"

  "routes should have correct path and method" in:
    AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.submit.url shouldBe AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url

  private final case class TestCaseForCya(
    application: AgentApplication,
    amlsType: String,
    expectedRedirect: Option[String] = None
  )

  object agentApplication:

    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

    val newSupervisorInFix: AgentApplicationLlp = riskingCompletedFixable
      .copy(
        riskingOutcomeEntity = Some(tdAll.riskingOutcomeEntityNewAmlsSupervisor)
      )

  // because our starting position is a complete AmlsDetails model, when we find a missing reg number we know it can only
  // be because the supervisor was changed (which unset the old reg number) and the user abandoned the journey without
  // supplying a new reg number, we redirect to supervisor when reg number is missing as it's hardwired to redirect to
  // reg number instead of CYA and this is the only way for the user to see/edit the supervisor in this scenario
  List(
    TestCaseForCya(
      application = agentApplication.riskingCompletedFixable,
      amlsType = "HMRC",
      expectedRedirect = None
    ),
    TestCaseForCya(
      application = agentApplication.newSupervisorInFix,
      amlsType = "HMRC",
      expectedRedirect = Some(AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show.url)
    )
  ).foreach: testCase =>
    if testCase.expectedRedirect.isEmpty then
      s"GET $path with complete amls details should return 200 and render page for ${testCase.amlsType}" in:
        ApplyStubHelper.stubsToSupplyBprToPage(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    else
      s"GET $path with incomplete amls details due to new supervisor should redirect to ${testCase.expectedRedirect.value} for ${testCase.amlsType}" in:
        ApplyStubHelper.stubsToSupplyBprToPage(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe Status.SEE_OTHER
        response.body[String] shouldBe Constants.EMPTY_STRING
        response.header("Location").value shouldBe testCase.expectedRedirect.value
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
