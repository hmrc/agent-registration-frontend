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
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.ObjectStoreStubs

class EvidenceDownloadUrlControllerISpec
extends ControllerSpec {

  override val baseUrl: String = "/api"
  val fileReference: String = tdAll.uploadId.value
  val downloadUrl: String = "https://object-store/download-url"
  val endpoint: String = s"/api/amls/evidence-download-url/$fileReference"
  val fileName = "test.pdf"

  s"GET $endpoint" should {
    "return OK and download url when evidence exists" in {
      AuthStubs.stubStrideAuth()
      ObjectStoreStubs.stubObjectStoreListObjects(
        directory = fileReference,
        fileName = fileName
      )
      ObjectStoreStubs.stubObjectStorePresignedDownloadUrl(
        downloadUrl = downloadUrl
      )

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.OK
      (response.json \ "downloadUrl").as[String] shouldBe downloadUrl
    }

    "return NOT_FOUND when evidence does not exist" in {
      AuthStubs.stubStrideAuth()
      ObjectStoreStubs.stubObjectStoreListObjectsNotFound(directory = fileReference)

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.NOT_FOUND
    }

    "return FORBIDDEN when user is not authenticated" in {
      AuthStubs.stubUnauthorized()

      val response: WSResponse = get(endpoint)
      response.status shouldBe Status.FORBIDDEN
    }
  }

}
