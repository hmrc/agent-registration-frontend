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
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker
import uk.gov.hmrc.objectstore.client.Md5Hash
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5
import uk.gov.hmrc.objectstore.client.Path

import java.time.Instant

object ObjectStoreStubs:

  private val stubResponse = ObjectSummaryWithMd5(
    location = Path.File(Path.Directory("object-store/object/my-folder"), "sample.pdf"),
    contentLength = 1000L,
    contentMd5 = Md5Hash("a3c2f1e38701bd2c7b54ebd7b1cd0dbc"),
    lastModified = Instant.now
  )

  def stubObjectStoreTransfer(response: ObjectSummaryWithMd5 = stubResponse): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/object-store/ops/upload-from-url"),
    responseStatus = 200,
    responseBody =
      Json.obj(
        "location" -> response.location.asUri,
        "contentLength" -> response.contentLength,
        "contentMD5" -> response.contentMd5.value,
        "lastModified" -> response.lastModified
      ).toString
  )

  def verifyObjectStoreTransfer(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/object-store/ops/upload-from-url"),
    count = count
  )
