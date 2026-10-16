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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs

import com.github.tomakehurst.wiremock.client.WireMock as wm
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesRequest
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.StubMaker

object TestRiskingStubs:

  def stubListRiskingResultsFiles(filenames: Seq[RiskingResultsFilename]): StubMapping = StubMaker.make(
    urlPattern = wm.urlPathEqualTo("/files-available/list/informationTypePlaceholder"),
    responseBody = filesJson(filenames)
  )

  def stubUnprocessedRiskingResultsFiles(filenames: Seq[RiskingResultsFilename]): StubMapping = StubMaker.make(
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/unprocessed-available-files"),
    responseBody = filesJson(filenames)
  )

  def stubViewRiskingResultsFile(
    filename: RiskingResultsFilename,
    content: JsValue
  ): StubMapping = StubMaker.make(
    urlPattern = fileUrl(filename),
    responseBody = Json.prettyPrint(content)
  )

  def stubViewRiskingResultsFileNotFound(filename: RiskingResultsFilename): StubMapping = StubMaker.make(
    urlPattern = fileUrl(filename),
    responseStatus = 404
  )

  def stubUploadRiskingResultsFile(
    filename: RiskingResultsFilename,
    content: JsValue
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = fileUrl(filename),
    requestBody = Some(wm.equalToJson(Json.prettyPrint(content)))
  )

  def stubUploadRiskingResultsFileFailure(filename: RiskingResultsFilename): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = fileUrl(filename),
    responseStatus = 500
  )

  def verifyUploadRiskingResultsFile(
    filename: RiskingResultsFilename,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = fileUrl(filename),
    count = count
  )

  def stubDeleteRiskingResultsFile(filename: RiskingResultsFilename): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = fileUrl(filename)
  )

  def verifyDeleteRiskingResultsFile(
    filename: RiskingResultsFilename,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = fileUrl(filename),
    count = count
  )

  def stubDeleteAllApplications(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/applications"),
    responseStatus = Status.NO_CONTENT
  )

  def verifyDeleteAllApplications(count: Int = 1): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.DELETE,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/applications"),
    count = count
  )

  def stubDeleteAllRiskingResultsFiles(): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/delete-all-risking-results-files")
  )

  def verifyDeleteAllRiskingResultsFiles(count: Int): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/delete-all-risking-results-files"),
    count = count
  )

  def stubFindRiskingResultsFiles(
    findRiskingResultsFilesRequest: FindRiskingResultsFilesRequest,
    findRiskingResultsFilesResponse: FindRiskingResultsFilesResponse
  ): StubMapping = StubMaker.make(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/find-risking-results-files/by-references"),
    requestBody = Some(wm.equalToJson(Json.prettyPrint(Json.toJson(findRiskingResultsFilesRequest)))),
    responseBody = Json.stringify(Json.toJson(findRiskingResultsFilesResponse))
  )

  def verifyFindRiskingResultsFiles(
    findRiskingResultsFilesRequest: FindRiskingResultsFilesRequest,
    count: Int = 1
  ): Unit = StubMaker.verify(
    httpMethod = StubMaker.HttpMethod.POST,
    urlPattern = wm.urlPathEqualTo(s"$testOnlyPath/find-risking-results-files/by-references"),
    requestBody = Some(wm.equalToJson(Json.prettyPrint(Json.toJson(findRiskingResultsFilesRequest)))),
    count = count
  )

  private val testOnlyPath: String = "/agent-registration-risking/test-only"

  private def fileUrl(filename: RiskingResultsFilename): UrlPattern = wm.urlPathEqualTo(s"$testOnlyPath/risking-results-file/${filename.value}")

  private def filesJson(filenames: Seq[RiskingResultsFilename]): String = Json.stringify(Json.toJson(filenames.map(filename =>
    Json.obj("filename" -> filename.value)
  )))
