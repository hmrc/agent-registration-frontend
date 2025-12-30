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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp

import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.ObjectStoreStubs

class AmlsEvidenceUploadControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/anti-money-laundering/evidence"
  private val resultPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-result"
  private val uploadErrorPath = "/agent-registration/apply/anti-money-laundering/evidence/error"

  private object agentApplication:

    val hmrcAmls: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .afterRegistrationNumberProvided

    val beforeAmlsExpiryDateProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterRegistrationNumberProvided

    val afterAmlsExpiryDateProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

    val afterUploadInitiated: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadInitiated

    val afterUploadScannedOk: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadScannedOk

    val afterUploadSucceeded: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadSucceeded

    val afterUploadFailed: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadFailed

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.AmlsEvidenceUploadController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.amls.AmlsEvidenceUploadController.showUploadResult shouldBe Call(
      method = "GET",
      url = resultPath
    )

  private object ExpectedStrings:

    private val heading = "Evidence of your anti-money laundering supervision"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val pendingTitle = "We are checking your upload - Apply for an agent services account - GOV.UK"
    val completeTitle = "Your upload is complete - Apply for an agent services account - GOV.UK"
    val virusTitle = "Your upload has failed scanning - Apply for an agent services account - GOV.UK"
    val tooLargeTitle = "Your upload is too large - Apply for an agent services account - GOV.UK"

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForInitialisingUpload(
      application = agentApplication.afterAmlsExpiryDateProvided,
      updatedApplication = agentApplication.afterUploadInitiated
    )
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.title
    ApplyStubHelper.verifyConnectorsForUploadInitiate()

  s"GET $path when expiry date is missing should redirect to the expiry date page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.beforeAmlsExpiryDateProvided)

    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.AmlsExpiryDateController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when supervisor is HMRC should redirect to check your answers" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.hmrcAmls)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe routes.CheckYourAnswersController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $resultPath when upload in progress should render the upload result page" in:
    ApplyStubHelper.stubsForUploadInProgress(
      application = agentApplication.afterUploadInitiated,
      uploadId = tdAll.uploadId
    )
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterUploadInitiated)
    val response: WSResponse = get(resultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.pendingTitle
    ApplyStubHelper.verifyConnectorsForUploadInProgress()

  s"GET $resultPath when upload is successfully scanned should transfer to object store and render the upload result page" in:
    ApplyStubHelper.stubsForUploadStatusChange(
      application = agentApplication.afterUploadScannedOk,
      updatedApplication = agentApplication.afterUploadSucceeded,
      uploadId = tdAll.uploadId,
      uploadStatus = tdAll.uploadedSuccessfully
    )
    ObjectStoreStubs.stubObjectStoreTransfer()
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.completeTitle
    ApplyStubHelper.verifyConnectorsForUploadResult()
    ObjectStoreStubs.verifyObjectStoreTransfer()

  s"GET $resultPath when upload has a virus should render the upload result page" in:
    ApplyStubHelper.stubsForUploadStatusChange(
      application = agentApplication.afterUploadInitiated,
      updatedApplication = agentApplication.afterUploadFailed,
      uploadId = tdAll.uploadId,
      uploadStatus = UploadStatus.Failed
    )
    val response: WSResponse = get(resultPath)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.virusTitle
    ApplyStubHelper.verifyConnectorsForUploadResult()

  s"GET $uploadErrorPath with params should render the error page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterAmlsExpiryDateProvided)
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=EntityTooLarge&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.tooLargeTitle
    ApplyStubHelper.verifyConnectorsForAuthAction()
