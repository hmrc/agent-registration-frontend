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
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepo
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class AmlsEvidenceUploadControllerCheckUploadStatusJsSpec
extends ControllerSpec:

  private object agentApplication:

    val afterAmlsExpiryDateProvided: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .afterAmlsExpiryDateProvided

  val path: String = AppRoutes.apply.amls.AmlsEvidenceUploadController.checkUploadStatusJs.url

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

      AuthStubs.stubAuthorise()
      AgentRegistrationStubs.stubGetAgentApplication(agentApplication.afterAmlsExpiryDateProvided)

      val response: WSResponse = get(path)
      response.status shouldBe tc.expectedHttpStatus
      val expectedHeaders = Map(
        "Access-Control-Allow-Origin" -> Seq(thisFrontendBaseUrl),
        "Access-Control-Allow-Credentials" -> Seq("true"),
        "Access-Control-Allow-Methods" -> Seq("GET, OPTIONS")
      )
      response.headers should contain allElementsOf expectedHeaders
      response.body[String] shouldBe Constants.EMPTY_STRING

      AuthStubs.verifyAuthorise()
      AgentRegistrationStubs.verifyGetAgentApplication()
