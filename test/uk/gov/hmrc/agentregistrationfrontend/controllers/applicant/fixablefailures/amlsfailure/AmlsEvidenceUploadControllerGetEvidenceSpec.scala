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

import play.api.libs.ws.WSResponse
import play.api.libs.ws.readableAsString
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.UpscanStubs

class AmlsEvidenceUploadControllerGetEvidenceSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val evidencePath = "/agent-registration/conditions-not-yet-met/anti-money-laundering/evidence"

  private object agentApplication:

    val whenSupervisorBodyIsHmrc: AgentApplication =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

    val whenSupervisorBodyIsNonHmrc: AgentApplication =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableNonHmrcAmls

    val afterUploadSucceeded: AgentApplication = whenSupervisorBodyIsNonHmrc // as this is a fix, there is existing evidence

    val afterUploadFailed: AgentApplication = whenSupervisorBodyIsNonHmrc // as this is a fix, there is existing evidence

  "routes should have correct paths and methods" in:
    AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.show shouldBe Call(
      method = "GET",
      url = evidencePath
    )

  private object ExpectedStrings:

    private val heading = "Upload evidence"
    val title: String = s"$heading - Apply for an agent services account - GOV.UK"

  s"GET $evidencePath when supervisor in fix is HMRC should redirect to check your answers" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.whenSupervisorBodyIsHmrc)

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url

    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $evidencePath should initialise upscan and upload and return 200 and render page" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.whenSupervisorBodyIsNonHmrc)
    UpscanStubs.stubUpscanInitiate(isFix = true)
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

    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
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

    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.afterUploadSucceeded)
    UpscanStubs.stubUpscanInitiate(isFix = true)

    val response: WSResponse = get(evidencePath)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.title

    uploadRepo
      .findLatestByInternalUserId(tdAll.internalUserId)
      .futureValue
      .value shouldBe tdAll.uploadInProgress withClue "when calling page it creates Upload record to track upload progress"

    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
    UpscanStubs.verifyUpscanInitiateRequest()
