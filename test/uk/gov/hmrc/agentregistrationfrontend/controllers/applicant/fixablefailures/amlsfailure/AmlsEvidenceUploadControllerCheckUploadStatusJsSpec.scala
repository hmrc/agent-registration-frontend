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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsEvidenceUploadControllerCheckUploadStatusJsSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private object agentApplication:

    val riskingCompletedFixableAmls: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableNonHmrcAmls

  val path: String = AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.checkUploadStatusJs.url

  case class TestCase(
    upload: Upload,
    expectedHttpStatus: Int
  )

  List(
    TestCase(
      upload = tdAll.uploadInProgress,
      expectedHttpStatus = Status.NO_CONTENT
    ),
    TestCase(
      upload = tdAll.uploadUploadedSuccessfully,
      expectedHttpStatus = Status.ACCEPTED
    ),
    TestCase(
      upload = tdAll.uploadFailed,
      expectedHttpStatus = Status.BAD_REQUEST
    )
  ).foreach: tc =>
    s"GET $path, if upload is in ${tc.upload.uploadStatus}, should return ${tc.expectedHttpStatus}" in:
      withClue("prerequisite to reflect that the Upload in proper state"):
        val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
        uploadRepo.drop().futureValue
        uploadRepo.upsert(tc.upload).futureValue

      ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixableAmls)

      val response: WSResponse = get(path)
      response.status shouldBe tc.expectedHttpStatus
      val expectedHeaders = Map(
        "Access-Control-Allow-Origin" -> Seq(thisFrontendBaseUrl),
        "Access-Control-Allow-Credentials" -> Seq("true"),
        "Access-Control-Allow-Methods" -> Seq("GET, OPTIONS")
      )
      response.headers should contain allElementsOf expectedHeaders
      response.body[String] shouldBe Constants.EMPTY_STRING

      ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
