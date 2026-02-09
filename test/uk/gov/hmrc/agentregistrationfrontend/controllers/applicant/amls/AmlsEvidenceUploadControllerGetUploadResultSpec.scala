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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.ObjectStoreStubs

class AmlsEvidenceUploadControllerGetUploadResultSpec
extends ControllerSpec:

  private val uploadResultPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-result"

  private object agentApplication:

    val afterAmlsExpiryDateProvided: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

    val afterUploadSucceeded: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadSucceeded

  "routes should have correct paths and methods" in:

    AppRoutes.apply.amls.AmlsEvidenceUploadController.showUploadResult shouldBe Call(
      method = "GET",
      url = uploadResultPath
    )

  private object ExpectedStrings:

    val titleInProgress: String = "We are checking your upload - Apply for an agent services account - GOV.UK"
    val titleUploadFailed: String = "Your upload has failed scanning - Apply for an agent services account - GOV.UK"
    val titleUploadFailedWithVirus: String = "Your upload has a virus - Apply for an agent services account - GOV.UK"
    val titleSucceeded: String = "Your upload is complete - Apply for an agent services account - GOV.UK"

  s"GET $uploadResultPath, if the Upload is in progress, should render appropriate message without updating any records" in:
    withClue("prerequisite to reflect that the Upload is in progress"):
      val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
      uploadRepo.drop().futureValue
      uploadRepo.upsert(tdAll.uploadInProgress).futureValue
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)

    val response: WSResponse = get(uploadResultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.titleInProgress
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $uploadResultPath, if the Upload failed, should render a appropriate message without updating any records" in:
    withClue("prerequisite to reflect that the Upload failed"):
      val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
      uploadRepo.drop().futureValue
      uploadRepo.upsert(tdAll.uploadFailed).futureValue
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)

    val response: WSResponse = get(uploadResultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.titleUploadFailed
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $uploadResultPath, if the Upload failed with a virus, should render a appropriate message without updating any records" in:
    withClue("prerequisite to reflect that the Upload failed"):
      val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
      uploadRepo.drop().futureValue
      uploadRepo.upsert(tdAll.uploadFailedWithVirus).futureValue
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)

    val response: WSResponse = get(uploadResultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.titleUploadFailedWithVirus
    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $uploadResultPath, if the Upload succeeded, should render appropriate message and idempotently update AgentApplication and transfer uploaded file to the ObjectStore" in:
    withClue("prerequisite to reflect that the Upload succeeded"):
      val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
      uploadRepo.drop().futureValue
      uploadRepo.upsert(tdAll.uploadUploadedSuccessfully).futureValue

    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)
    ObjectStoreStubs.stubObjectStoreTransfer()
    AgentRegistrationStubs.stubUpdateAgentApplication(agentApplication.afterUploadSucceeded)

    val response: WSResponse = get(uploadResultPath)
    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.titleSucceeded

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    ObjectStoreStubs.verifyObjectStoreTransfer()
    AgentRegistrationStubs.verifyUpdateAgentApplication()

    withClue("idempotence check - second page load doesn't trigger unnecessary updates"):
      AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUploadSucceeded)
      val response: WSResponse = get(uploadResultPath)
      response.status shouldBe 200
      response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.titleSucceeded

      AuthStubs.verifyAuthorise(2)
      AgentRegistrationStubs.verifyGetAgentApplication(2)
      ObjectStoreStubs.verifyObjectStoreTransfer() // still one!
      AgentRegistrationStubs.verifyUpdateAgentApplication() // still one!
