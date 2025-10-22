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
import com.softwaremill.quicklens.*
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val path = "/agent-registration/apply/anti-money-laundering/check-your-answers"

  "route should have correct path and method" in:
    routes.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  // when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies
  // and no evidence or expiry date is required to be considered complete
  private val completeHmrcApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = AmlsCode("HMRC"),
      amlsRegistrationNumber = Some(AmlsRegistrationNumber("XAML00000123456")),
      amlsExpiryDate = None,
      amlsEvidence = None
    )))
  private val completeNonHmrcApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = AmlsCode("FCA"),
      amlsRegistrationNumber = Some(AmlsRegistrationNumber("1234567890")),
      amlsExpiryDate = Some(tdAll.validAmlsExpiryDate),
      amlsEvidence = Some(tdAll.amlsUploadDetailsSuccess)
    )))
  private val incompleteHmrcApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = AmlsCode("HMRC"),
      amlsRegistrationNumber = None
    )))
  private val incompleteNonHmrcApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = AmlsCode("FCA"),
      amlsRegistrationNumber = Some(AmlsRegistrationNumber("1234567890")),
      amlsExpiryDate = Some(tdAll.validAmlsExpiryDate),
      amlsEvidence = None
    )))

  private case class TestCaseForCya(
    application: AgentApplication,
    amlsType: String,
    isComplete: Boolean
  )

  List(
    TestCaseForCya(
      application = completeHmrcApplication,
      amlsType = "HMRC",
      isComplete = true
    ),
    TestCaseForCya(
      application = completeNonHmrcApplication,
      amlsType = "non-HMRC",
      isComplete = true
    ),
    TestCaseForCya(
      application = incompleteHmrcApplication,
      amlsType = "HMRC",
      isComplete = false
    ),
    TestCaseForCya(
      application = incompleteNonHmrcApplication,
      amlsType = "non-HMRC",
      isComplete = false
    )
  ).foreach: testCase =>
    if testCase.isComplete then
      s"GET $path with complete amls details should return 200 and render page for ${testCase.amlsType}" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 200
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
    else
      s"GET $path with incomplete amls details should redirect to the start of the amls journey for ${testCase.amlsType}" in:
        AuthStubs.stubAuthorise()
        AgentRegistrationStubs.stubGetAgentApplication(testCase.application)
        val response: WSResponse = get(path)

        response.status shouldBe 303
        response.header("Location").value shouldBe routes.AmlsSupervisorController.show.url
