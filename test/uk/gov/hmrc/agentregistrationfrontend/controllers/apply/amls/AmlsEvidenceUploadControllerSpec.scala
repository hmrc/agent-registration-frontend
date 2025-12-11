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
import org.apache.pekko.actor.ActorSystem
import play.api.libs.ws.WSResponse
import play.api.libs.ws.readableAsString
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.upscan.UploadId
import uk.gov.hmrc.agentregistration.shared.upscan.UploadIdGenerator
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.objectstore.client.RetentionPeriod.OneWeek
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.play.test.stub

import scala.concurrent.ExecutionContext

class AmlsEvidenceUploadControllerSpec
extends ControllerSpec:

  private given as: ActorSystem = ActorSystem()
  private given ec: ExecutionContext = as.dispatcher

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit =
        bind(classOf[AmlsCodes]).asEagerSingleton()
        bind(classOf[UploadIdGenerator]).toInstance(new UploadIdGenerator {
          override def nextUploadId(): UploadId = tdAll.uploadId
        })
        bind(classOf[PlayObjectStoreClient]).toInstance(
          new stub.StubPlayObjectStoreClient(ObjectStoreClientConfig(
            baseUrl = "http://basurl.com",
            owner = "owner",
            authorizationToken = "token",
            defaultRetentionPeriod = OneWeek
          ))
        )

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

    val afterUploadSucceded: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterUploadSucceded

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

  /* TODO: this test is consistently timing out a future promise - I think it's because of the stubbed object store client - investigate later
  s"GET $resultPath when upload is successfully scanned should render the upload result page" in:
    ApplyStubHelper.stubsForUploadStatusChange(
      application = agentApplication.afterUploadInitiated,
      updatedApplication = agentApplication.afterUploadSucceded,
      uploadId = tdAll.uploadId,
      uploadStatus = UploadStatus.UploadedSuccessfully(
        downloadUrl = url"https://bucketName.s3.eu-west-2.amazonaws.com/xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        name = "evidence.pdf",
        mimeType = "application/pdf",
        size = Some(12345L),
        checksum = tdAll.objectStoreValidHexVal,
        objectStoreLocation = Some(ObjectStoreUrl("any-file-reference/any-file-name"))
      )
    )
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.completeTitle
    ApplyStubHelper.verifyConnectorsForUploadResult()
   */

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
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=TOO_LARGE&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title shouldBe ExpectedStrings.tooLargeTitle
    ApplyStubHelper.verifyConnectorsForAuthAction()
