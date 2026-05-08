/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.ObjectStoreStubs

class EvidenceDownloadUrlControllerISpec
extends ControllerSpec:

  override val baseUrl: String = "/api"
  val fileReference: FileUploadReference = tdAll.fileUploadReference
  val downloadUrl: String = "https://object-store/download-url"
  val endpoint: String = s"/api/amls/evidence-download-url/${fileReference.value}"
  val fileName = "test.pdf"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.amls.api.EvidenceDownloadUrlController.evidenceDownloadUrl(fileReference) shouldBe Call(
      method = "GET",
      url = endpoint
    )

  s"GET $endpoint" should:
    "return OK and download url when evidence exists" in:
      AuthStubs.stubStrideAuth()
      ObjectStoreStubs.stubObjectStoreListObjects(
        fileUploadReference = fileReference,
        fileName = fileName
      )
      ObjectStoreStubs.stubObjectStorePresignedDownloadUrl(
        downloadUrl = downloadUrl
      )

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.OK
      (response.json \ "downloadUrl").as[String] shouldBe downloadUrl
      ObjectStoreStubs.verifyObjectStoreListObjects(fileUploadReference = fileReference)
      ObjectStoreStubs.verifyObjectStorePresignedDownloadUrl()

    "return NOT_FOUND when evidence does not exist" in:
      AuthStubs.stubStrideAuth()
      ObjectStoreStubs.stubObjectStoreListObjectsNotFound(fileUploadReference = fileReference)

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.NO_CONTENT
      ObjectStoreStubs.verifyObjectStoreListObjects(fileUploadReference = fileReference)
      ObjectStoreStubs.verifyObjectStorePresignedDownloadUrl(count = 0)

    "return FORBIDDEN when user is not authenticated" in:
      AuthStubs.stubUnauthorized()

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.FORBIDDEN
      ObjectStoreStubs.verifyObjectStoreListObjects(fileUploadReference = fileReference, count = 0)
      ObjectStoreStubs.verifyObjectStorePresignedDownloadUrl(count = 0)
