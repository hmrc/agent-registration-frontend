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

import play.api.libs.ws.WSResponse
import play.api.libs.ws.readableAsString
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.UpscanStubs

class AmlsEvidenceUploadControllerGetEvidenceSpec
extends ControllerSpec:

  private val evidencePath = "/agent-registration/apply/anti-money-laundering/evidence"

  private object agentApplication:

    val whenSupervisorBodyIsHmrc: AgentApplication =
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

    val afterUploadSucceeded: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadSucceeded

    val afterUploadFailed: AgentApplication = afterAmlsExpiryDateProvided

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.AmlsEvidenceUploadController.showAmlsEvidenceUploadPage shouldBe Call(
      method = "GET",
      url = evidencePath
    )

  private object ExpectedStrings:

    private val heading = "Evidence of your anti-money laundering supervision"
    val title: String = s"$heading - Apply for an agent services account - GOV.UK"

  s"GET $evidencePath when expiry date is missing should redirect to the expiry date page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.beforeAmlsExpiryDateProvided)

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.AmlsExpiryDateController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $evidencePath when supervisor is HMRC should redirect to check your answers" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.whenSupervisorBodyIsHmrc)

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.amls.CheckYourAnswersController.show.url

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()

  s"GET $evidencePath should initialise upscan and upload and return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)
    UpscanStubs.stubUpscanInitiate()
    val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
    uploadRepo.drop().futureValue
    uploadRepo.findLatestByInternalUserId(tdAll.internalUserId).futureValue shouldBe None withClue "sanity check, no Uploads before we call the endpoint"

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.title

    uploadRepo
      .findLatestByInternalUserId(tdAll.internalUserId)
      .futureValue
      .value shouldBe tdAll.uploadInProgress withClue "when calling page it creates Upload record to track upload progress"

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    UpscanStubs.verifyUpscanInitiateRequest()

  s"GET $evidencePath can start new upload process when file was already uploaded" in:
    val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
    withClue("Emulate previously succeeded upload") {
      uploadRepo.drop().futureValue
      val previouslySucceededUpload: Upload = tdAll.uploadUploadedSuccessfully.copy(
        _id = UploadId("previouslySucceededUpload-id-12345"),
        createdAt = tdAll.uploadUploadedSuccessfully.createdAt.minusSeconds(120)
      )
      uploadRepo.upsert(previouslySucceededUpload).futureValue
      uploadRepo.findLatestByInternalUserId(tdAll.internalUserId).futureValue.value shouldBe previouslySucceededUpload
    }

    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterUploadSucceeded)
    UpscanStubs.stubUpscanInitiate()

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.title

    uploadRepo
      .findLatestByInternalUserId(tdAll.internalUserId)
      .futureValue
      .value shouldBe tdAll.uploadInProgress withClue "when calling page it creates Upload record to track upload progress"

    AuthStubs.verifyAuthorise()
    AgentRegistrationStubs.verifyGetAgentApplication()
    UpscanStubs.verifyUpscanInitiateRequest()
