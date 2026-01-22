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
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object UpscanStubs:

  def requestJson(uploadId: UploadId): String = {
    // language=JSON
    s"""
   {
       "callbackUrl": "${ISpec.selfBaseUrl}/api/amls/process-notification-from-upscan/${uploadId.value}",
       "successRedirect": "${ISpec.thisFrontendBaseUrl}/agent-registration/apply/anti-money-laundering/evidence/upload-result",
       "errorRedirect": "${ISpec.thisFrontendBaseUrl}/agent-registration/apply/anti-money-laundering/evidence/error",
       "maximumFileSize": 5242880
   }
   """
  }

  def responseJson(fileUploadReference: FileUploadReference): String =
    // language=JSON
    s"""
   {
       "reference": "${fileUploadReference.value}",
       "uploadRequest": {
           "href": "https://bucketName.s3.eu-west-2.amazonaws.com",
           "fields": {
               "x-amz-meta-callback-url": "${ISpec.thisFrontendBaseUrl}/agent-registration/application/amls/upscan-callback",
               "x-amz-date": "yyyyMMddThhmmssZ",
               "x-amz-credential": "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
               "x-amz-algorithm": "AWS4-HMAC-SHA256",
               "key": "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
               "acl": "private",
               "x-amz-signature": "xxxx",
               "x-amz-meta-consuming-service": "agent-registration-frontend",
               "policy": "xxxxxxxx=="
           }
       }
   }
   """

  def stubUpscanInitiate(
    fileUploadReference: FileUploadReference = TdAll.tdAll.fileUploadReference,
    uploadId: UploadId = TdAll.tdAll.uploadId
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/upscan/v2/initiate"),
    requestBody = Some(equalToJson(
      requestJson(uploadId),
      true,
      true
    )),
    responseStatus = 200,
    responseBody = responseJson(fileUploadReference)
  )

  def verifyUpscanInitiateRequest(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlEqualTo("/upscan/v2/initiate"),
    count = count
  )
