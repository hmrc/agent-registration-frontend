/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

import java.time.Instant

object ObjectStoreStubs:

  def stubObjectStoreTransfer(response: JsObject = TdAll.tdAll.objectStoreUploadResponse): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/object-store/ops/upload-from-url"),
    responseStatus = 200,
    responseBody = Json.prettyPrint(response)
  )

  def verifyObjectStoreTransfer(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/object-store/ops/upload-from-url"),
    count = count
  )

  def stubObjectStoreListObjects(
    fileUploadReference: FileUploadReference,
    fileName: String
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/object-store/list/agent-registration-frontend/${fileUploadReference.value}"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "objectSummaries" -> Json.arr(
          Json.obj(
            "location" -> s"/agent-registration-frontend/${fileUploadReference.value}/$fileName",
            "contentLength" -> 1234,
            "lastModified" -> Instant.now()
          )
        )
      ).toString
  )

  def stubObjectStoreListObjectsNotFound(fileUploadReference: FileUploadReference): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/object-store/list/agent-registration-frontend/${fileUploadReference.value}"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "objectSummaries" -> Json.arr()
      ).toString
  )

  def verifyObjectStoreListObjects(
    fileUploadReference: FileUploadReference,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.GET,
    urlPattern = wm.urlEqualTo(s"/object-store/list/agent-registration-frontend/${fileUploadReference.value}"),
    count = count
  )

  def stubObjectStorePresignedDownloadUrl(
    downloadUrl: String = "https://object-store/download-url"
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/object-store/ops/presigned-url"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "downloadUrl" -> downloadUrl,
        "contentLength" -> 1234,
        "contentMD5" -> "abc123"
      ).toString
  )

  def verifyObjectStorePresignedDownloadUrl(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo(s"/object-store/ops/presigned-url"),
    count = count
  )
