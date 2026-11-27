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

import org.apache.pekko.util.ByteString
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.upload.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.ObjectStoreStubs

class EvidenceDownloadControllerISpec
extends ControllerSpec:

  override val baseUrl: String = "/api"
  val fileReference: FileUploadReference = tdAll.fileUploadReference
  val evidenceDownloadRoute: String = s"/api/amls/evidence/${fileReference.value}"
  val fileName = "test.pdf"

  s"GET $evidenceDownloadRoute" should:
    "have correct path and method" in:
      AppRoutes.apply.amls.api.EvidenceDownloadController.evidenceDownload(fileReference) shouldBe Call(
        method = "GET",
        url = evidenceDownloadRoute
      )

    "return OK and return file when evidence exists" in:
      AuthStubs.stubStrideAuth
      ObjectStoreStubs.stubObjectStoreListObjects(
        fileUploadReference = fileReference,
        fileName = fileName
      )
      ObjectStoreStubs.stubObjectStoreGetObject(
        fileUploadReference = fileReference,
        fileName = fileName,
        fileContent = "test content".getBytes
      )

      val response: WSResponse = get(evidenceDownloadRoute)
      response.status shouldBe Status.OK
      response.bodyAsBytes shouldBe ByteString("test content")
      response.header(HeaderNames.CONTENT_MD5).value shouldBe "lHP90NiApDwht3eNNIchVw=="
      response.header(HeaderNames.CONTENT_TYPE).value shouldBe "application/octet-stream"
      response.header(HeaderNames.LAST_MODIFIED).value shouldBe "Thu, 26 Nov 2026 10:03:22 GMT"
      response.header(HeaderNames.CONTENT_LENGTH).value shouldBe "12"

    "return NOT_FOUND when evidence does not exist" in:
      AuthStubs.stubStrideAuth
      ObjectStoreStubs.stubObjectStoreListObjectsNotFound(fileUploadReference = fileReference)

      val response: WSResponse = get(evidenceDownloadRoute)
      response.status shouldBe Status.NOT_FOUND
      ObjectStoreStubs.verifyObjectStoreListObjects(fileUploadReference = fileReference)

    "return FORBIDDEN when user is not authenticated" in:
      AuthStubs.stubUnauthorized()

      val response: WSResponse = get(evidenceDownloadRoute)
      response.status shouldBe Status.FORBIDDEN
      ObjectStoreStubs.verifyObjectStoreListObjects(fileUploadReference = fileReference, count = 0)
