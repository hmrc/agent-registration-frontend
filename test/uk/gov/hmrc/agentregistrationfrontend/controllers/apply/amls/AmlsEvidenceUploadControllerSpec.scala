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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import com.google.inject.AbstractModule
import play.api.libs.ws.WSResponse
import play.api.libs.ws.readableAsString
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.UpscanStubs

class AmlsEvidenceUploadControllerSpec
extends ControllerSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  private val path = "/agent-registration/apply/anti-money-laundering/evidence"
  private val resultPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-result"
  private val uploadErrorPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-error"

  private object agentApplication:

    val hmrcAmls: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

    val beforeAmlsExpiryDateProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterRegistrationNumberProvided

    val afterAmlsExpiryDateProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

    val afterUploadInitiated: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadInitiated

    val afterUploadSucceded: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadSucceded

    val afterUploadFailed: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadFailed

  "routes should have correct paths and methods" in:
    routes.AmlsEvidenceUploadController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.AmlsEvidenceUploadController.showResult shouldBe Call(
      method = "GET",
      url = resultPath
    )

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)
    UpscanStubs.stubUpscanInitiateResponse()
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterUploadInitiated)
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe "Evidence of your anti-money laundering supervision - Apply for an agent services account - GOV.UK"

  s"GET $path when expiry date is missing should redirect to the expiry date page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeAmlsExpiryDateProvided)

    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.AmlsExpiryDateController.show.url

  s"GET $path when supervisor is HMRC should redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.hmrcAmls)

    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url

  s"GET $resultPath when upload in progress should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUploadInitiated)
    val response: WSResponse = get(resultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe "We are checking your upload - Apply for an agent services account - GOV.UK"

  s"GET $resultPath when upload is successfully scanned should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUploadSucceded)
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload is complete - Apply for an agent services account - GOV.UK"

  s"GET $resultPath when upload has a virus should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUploadFailed)
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload has a virus - Apply for an agent services account - GOV.UK"

  s"GET $uploadErrorPath with params should direct to the error page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=TOO_LARGE&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Upload Error - Apply for an agent services account - GOV.UK"
