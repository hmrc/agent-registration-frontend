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
import sttp.model.Uri.UriContext
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.upscan.Reference
import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistration.shared.upscan.ObjectStoreUrl
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.UpscanStubs

class AmlsEvidenceUploadControllerSpec
extends ControllerSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  private val applicationFactory = app.injector.instanceOf[ApplicationFactory]
  private val path = "/agent-registration/apply/anti-money-laundering/evidence"
  private val resultPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-result"
  private val uploadErrorPath = "/agent-registration/apply/anti-money-laundering/evidence/upload-error"
  private val fakeAgentApplication: AgentApplication = applicationFactory
    .makeNewAgentApplication(tdAll.internalUserId)
    .modify(_.amlsDetails)
    .setTo(Some(AmlsDetails(
      supervisoryBody = AmlsCode("FCA"),
      amlsRegistrationNumber = Some(AmlsRegistrationNumber("XAML00000123456")),
      amlsExpiryDate = Some(tdAll.validAmlsExpiryDate),
      amlsEvidence = None
    )))
  private val fakeAgentApplicationUploadInProgress: AgentApplication = fakeAgentApplication
    .modify(_.amlsDetails.each.amlsEvidence)
    .setTo(Some(
      UploadDetails(
        reference = Reference("test-file-reference"),
        status = UploadStatus.InProgress
      )
    ))
  private val fakeAgentApplicationComplete: AgentApplication = fakeAgentApplication
    .modify(_.amlsDetails.each.amlsEvidence)
    .setTo(Some(
      UploadDetails(
        reference = Reference("test-file-reference"),
        status = UploadStatus.UploadedSuccessfully(
          downloadUrl = ObjectStoreUrl(uri"https://bucketName.s3.eu-west-2.amazonaws.com/xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
          name = "evidence.pdf",
          mimeType = "application/pdf",
          size = Some(12345L),
          checksum = "md5:1B2M2Y8AsgTpgAmY7PhCfg=="
        )
      )
    ))
  private val fakeAgentApplicationVirus: AgentApplication = fakeAgentApplication
    .modify(_.amlsDetails.each.amlsEvidence)
    .setTo(Some(
      UploadDetails(
        reference = Reference("test-file-reference"),
        status = UploadStatus.Failed(
          failureReason = "QUARANTINE"
        )
      )
    ))

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
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    UpscanStubs.stubUpscanInitiateResponse()
    AgentRegistrationStubs.stubUpdateAgentApplication(fakeAgentApplicationUploadInProgress)
    val response: WSResponse = get(path)
    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Evidence of your anti-money laundering supervision - Apply for an agent services account - GOV.UK"

  s"GET $resultPath when upload in progress should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplicationUploadInProgress)
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "We are checking your upload - Apply for an agent services account - GOV.UK"

  s"GET $resultPath when upload is successfully scanned should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplicationComplete)
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload is complete - Apply for an agent services account - GOV.UK"

  s"GET $resultPath when upload has a virus should redirect to the upload result page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplicationVirus)
    val response: WSResponse = get(resultPath)

    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Your upload has a virus - Apply for an agent services account - GOV.UK"

  s"GET $uploadErrorPath with params should direct to the error page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubApplicationInProgress(fakeAgentApplication)
    val response: WSResponse = get(s"$uploadErrorPath?key=reference&errorRequestId=1&errorCode=TOO_LARGE&errorMessage=The%20file%20is%20too%20large")
    response.status shouldBe 200
    response.parseBodyAsJsoupDocument.title shouldBe "Upload Error - Apply for an agent services account - GOV.UK"
