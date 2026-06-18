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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsSupervisorControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = "/agent-registration/conditions-not-yet-met/anti-money-laundering/supervisor-name"

  object agentApplication:

    val riskingCompletedFixable: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

    val newSupervisorInFix: AgentApplicationLlp = riskingCompletedFixable
      .copy(
        riskingOutcomeEntity = Some(tdAll.riskingOutcomeEntityNewAmlsSupervisor)
      )

  private object ExpectedStrings:

    private val heading = "What is the name of the supervisory body for Test Company Name?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val requiredError = "Enter a name and choose your supervisor from the list"

  "routes should have correct paths and methods" in:
    AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.submit.url shouldBe AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixable)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when supervisory body already stored should return 200 and render page with previous answer selected" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixable)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    doc.select("select[name=amlsSupervisoryBody] option[selected]")
      .attr("value") shouldBe "HMRC"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"POST $path with valid selection should redirect to the next page" in:
    ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
      application = agentApplication.riskingCompletedFixable,
      updatedApplication = agentApplication.newSupervisorInFix
    )
    val response: WSResponse = post(path)(Map(AmlsCodeForm.key -> Seq("HMRC")))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForSuccessfulUpdateWithBpr()

  s"POST $path without valid selection should return 400" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixable)
    val response: WSResponse =
      post(path)(
        Map(
          AmlsCodeForm.key -> Seq(Constants.EMPTY_STRING)
        )
      )

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.errorTitle
    doc.mainContent.select(s"#${AmlsCodeForm.key}-error").text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
