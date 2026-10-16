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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import play.api.libs.json.JsArray
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestRiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.TestAgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.TestRiskingStubs

import scala.jdk.CollectionConverters.*

class TestOnlyControllerSpec
extends ControllerSpec:

  private val resetPath: String = "/agent-registration/test-only/reset"
  private val resultsFilesPath: String = "/agent-registration/test-only/risking/risking-results-files"
  private val combinePath: String = "/agent-registration/test-only/risking/risking-results-files/combine"
  private val combineAllPath: String = "/agent-registration/test-only/risking/risking-results-files/combine-all-unprocessed"
  private val deletePath: String = "/agent-registration/test-only/risking/risking-results-files/delete"
  private val deleteConfirmPath: String = "/agent-registration/test-only/risking/risking-results-files/delete-confirm"
  private val deleteAllProcessedConfirmPath: String = "/agent-registration/test-only/risking/risking-results-files/delete-all-processed-confirm"
  private def recordsPath(filename: RiskingResultsFilename): String = s"/agent-registration/test-only/risking/risking-results-files/${filename.value}/records"

  "routes should have correct paths and methods" in:
    AppRoutes.testOnly.TestOnlyController.resetDatabase shouldBe Call(
      method = "POST",
      url = resetPath
    )
    AppRoutes.testOnly.TestOnlyController.showSubmittedRiskingResultsFiles shouldBe Call(
      method = "GET",
      url = resultsFilesPath
    )
    AppRoutes.testOnly.TestOnlyController.combineRiskingResultsFiles shouldBe Call(
      method = "POST",
      url = combinePath
    )
    AppRoutes.testOnly.TestOnlyController.combineAllUnprocessedRiskingResultsFiles shouldBe Call(
      method = "POST",
      url = combineAllPath
    )
    AppRoutes.testOnly.TestOnlyController.confirmDeleteSelectedRiskingResultsFiles shouldBe Call(
      method = "POST",
      url = deleteConfirmPath
    )
    AppRoutes.testOnly.TestOnlyController.deleteSelectedRiskingResultsFiles shouldBe Call(
      method = "POST",
      url = deletePath
    )
    AppRoutes.testOnly.TestOnlyController.confirmDeleteAllProcessedRiskingResultsFiles shouldBe Call(
      method = "POST",
      url = deleteAllProcessedConfirmPath
    )
    AppRoutes.testOnly.TestOnlyController.showRiskingResultsFileRecords(entityFile) shouldBe Call(
      method = "GET",
      url = recordsPath(entityFile)
    )

  s"POST $resetPath should delete all applications, risking records and risking results files" in:
    TestAgentRegistrationStubs.stubDeleteAllApplications()
    TestRiskingStubs.stubDeleteAllApplications()
    TestRiskingStubs.stubDeleteAllRiskingResultsFiles()

    val response: WSResponse = post(resetPath)(Map.empty)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.testOnly.TestOnlyController.showTestOnlyHub.url
    TestAgentRegistrationStubs.verifyDeleteAllApplications()
    TestRiskingStubs.verifyDeleteAllApplications()
    TestRiskingStubs.verifyDeleteAllRiskingResultsFiles(count = 1)

  s"GET $resultsFilesPath should list each file with its status, reading no file contents" in:
    stubResultsFilesPage()

    val response: WSResponse = get(resultsFilesPath)

    response.status shouldBe Status.OK
    val page = response.parseBodyAsJsoupDocument
    page.selectOrFail(".govuk-checkboxes__item").eachText().asScala.toList shouldBe List(
      s"${entityFile.value} Unprocessed View file Records Loading…",
      s"${twoRecordFile.value} Processed View file Records Loading…"
    )
    page.selectOrFail("details[data-records-url]").eachAttr("data-records-url").asScala.toList shouldBe List(
      recordsPath(entityFile),
      recordsPath(twoRecordFile)
    )
    page.selectOrFail(".govuk-button-group button").eachText().asScala.toList shouldBe List(
      "Combine selected unprocessed files",
      "Combine all unprocessed files",
      "Delete selected processed files",
      "Delete all processed files"
    )
    page.selectOrFail(".govuk-button-group button.govuk-button--secondary").attr("formaction") shouldBe combineAllPath
    page.selectOrFail(".govuk-button-group button.govuk-button--warning").eachAttr("formaction").asScala.toList shouldBe List(
      deleteConfirmPath,
      deleteAllProcessedConfirmPath
    )
    val dangerZone: Element = page.selectOrFail("details#danger-zone").selectOnlyOneElementOrFail()
    dangerZone.hasAttr("open") shouldBe false
    dangerZone.selectOrFail(".govuk-details__summary-text").text() shouldBe "Danger zone Destructive"
    dangerZone.selectOrFail(".govuk-details__summary-text .govuk-tag--red").text() shouldBe "Destructive"
    dangerZone.selectOrFail("button").eachText().asScala.toList shouldBe List(
      "Delete selected processed files",
      "Delete all processed files"
    )

  s"GET ${recordsPath(twoRecordFile)} should list who the records in the file are about" in:
    TestRiskingStubs.stubViewRiskingResultsFile(twoRecordFile, entityRecords ++ individualRecords)

    val response: WSResponse = get(recordsPath(twoRecordFile))

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.selectOrFail("li").eachText().asScala.toList shouldBe List(
      "Application XARN0000001",
      "Person PERS0000001"
    )

  s"GET ${recordsPath(largeFile)} should list the first 20 records of a larger file" in:
    TestRiskingStubs.stubViewRiskingResultsFile(largeFile, largeFileRecords)

    val response: WSResponse = get(recordsPath(largeFile))

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.selectOrFail("li").eachText().asScala.toList shouldBe ((0 until 20).map(i =>
      f"Application XARN$i%07d"
    ) :+ "and 2 more").toList

  s"GET ${recordsPath(entityFile)} should be not found when the file does not exist" in:
    TestRiskingStubs.stubViewRiskingResultsFileNotFound(entityFile)

    val response: WSResponse = get(recordsPath(entityFile))

    response.status shouldBe Status.NOT_FOUND
    response.parseBodyAsJsoupDocument.text() shouldBe s"Risking results file not found: ${entityFile.value}"

  s"POST $combinePath should combine the selected files into one, then delete only those files" in:
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile, individualFile))
    TestRiskingStubs.stubViewRiskingResultsFile(entityFile, entityRecords)
    TestRiskingStubs.stubViewRiskingResultsFile(individualFile, individualRecords)
    TestRiskingStubs.stubUploadRiskingResultsFile(combinedFile, entityRecords ++ individualRecords)
    TestRiskingStubs.stubDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.stubDeleteRiskingResultsFile(individualFile)

    val response: WSResponse = post(combinePath)(Map("filenames[]" -> Seq(entityFile.value, individualFile.value)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.testOnly.TestOnlyController.showSubmittedRiskingResultsFiles.url
    TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(individualFile)
    TestRiskingStubs.verifyDeleteAllRiskingResultsFiles(count = 0)

  s"POST $combinePath should delete nothing when the combined file cannot be uploaded" in:
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile, individualFile))
    TestRiskingStubs.stubViewRiskingResultsFile(entityFile, entityRecords)
    TestRiskingStubs.stubViewRiskingResultsFile(individualFile, individualRecords)
    TestRiskingStubs.stubUploadRiskingResultsFileFailure(combinedFile)

    val response: WSResponse = post(combinePath)(Map("filenames[]" -> Seq(entityFile.value, individualFile.value)))

    response.status shouldBe Status.INTERNAL_SERVER_ERROR
    TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(entityFile, count = 0)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(individualFile, count = 0)

  s"POST $combinePath with a processed file selected should ask for unprocessed files only" in:
    stubResultsFilesPage()

    val response: WSResponse = post(combinePath)(Map("filenames[]" -> Seq(entityFile.value, twoRecordFile.value)))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "Select only unprocessed files to combine."
    TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile, count = 0)

  Seq(
    "no file" -> Seq.empty[RiskingResultsFilename],
    "one file" -> Seq(entityFile)
  ).foreach:
    (
      description,
      selectedFiles
    ) =>
      s"POST $combinePath with $description selected should ask for at least 2 files" in:
        stubResultsFilesPage()

        val response: WSResponse = post(combinePath)(Map("filenames[]" -> selectedFiles.map(_.value)))

        response.status shouldBe Status.BAD_REQUEST
        response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "Select at least 2 files to combine."
        TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile, count = 0)

  s"POST $combineAllPath should combine every unprocessed file into one, then delete only those files" in:
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile, individualFile))
    TestRiskingStubs.stubViewRiskingResultsFile(entityFile, entityRecords)
    TestRiskingStubs.stubViewRiskingResultsFile(individualFile, individualRecords)
    TestRiskingStubs.stubUploadRiskingResultsFile(combinedFile, entityRecords ++ individualRecords)
    TestRiskingStubs.stubDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.stubDeleteRiskingResultsFile(individualFile)

    val response: WSResponse = post(combineAllPath)(Map.empty)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.testOnly.TestOnlyController.showSubmittedRiskingResultsFiles.url
    TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(individualFile)
    TestRiskingStubs.verifyDeleteAllRiskingResultsFiles(count = 0)

  s"POST $combineAllPath with fewer than 2 unprocessed files should say so" in:
    stubResultsFilesPage()

    val response: WSResponse = post(combineAllPath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "There must be at least 2 unprocessed files to combine."
    TestRiskingStubs.verifyUploadRiskingResultsFile(combinedFile, count = 0)

  s"POST $deletePath should delete the selected files one by one" in:
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq.empty)
    TestRiskingStubs.stubDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.stubDeleteRiskingResultsFile(individualFile)

    val response: WSResponse = post(deletePath)(Map("filenames[]" -> Seq(entityFile.value, individualFile.value)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.testOnly.TestOnlyController.showSubmittedRiskingResultsFiles.url
    TestRiskingStubs.verifyDeleteRiskingResultsFile(entityFile)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(individualFile)
    TestRiskingStubs.verifyDeleteAllRiskingResultsFiles(count = 0)

  s"POST $deletePath with an unprocessed file selected should ask for processed files only" in:
    stubResultsFilesPage()

    val response: WSResponse = post(deletePath)(Map("filenames[]" -> Seq(entityFile.value, twoRecordFile.value)))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "Select only processed files to delete."
    TestRiskingStubs.verifyDeleteRiskingResultsFile(entityFile, count = 0)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(twoRecordFile, count = 0)

  s"POST $deletePath with no file selected should ask for a file" in:
    stubResultsFilesPage()

    val response: WSResponse = post(deletePath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "Select at least 1 file to delete."
    TestRiskingStubs.verifyDeleteAllRiskingResultsFiles(count = 0)

  s"POST $deleteConfirmPath should ask to confirm deleting the selected files, deleting nothing yet" in:
    stubResultsFilesPage()

    val response: WSResponse = post(deleteConfirmPath)(Map("filenames[]" -> Seq(twoRecordFile.value)))

    response.status shouldBe Status.OK
    assertDeleteConfirmationPage(
      response,
      List(twoRecordFile),
      deleteButtonText = "Delete 1 file"
    )
    TestRiskingStubs.verifyDeleteRiskingResultsFile(twoRecordFile, count = 0)

  s"POST $deleteConfirmPath with an unprocessed file selected should ask for processed files only" in:
    stubResultsFilesPage()

    val response: WSResponse = post(deleteConfirmPath)(Map("filenames[]" -> Seq(entityFile.value, twoRecordFile.value)))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "Select only processed files to delete."

  s"POST $deleteAllProcessedConfirmPath should ask to confirm deleting every processed file, deleting nothing yet" in:
    TestRiskingStubs.stubListRiskingResultsFiles(Seq(
      entityFile,
      individualFile,
      twoRecordFile
    ))
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile))

    val response: WSResponse = post(deleteAllProcessedConfirmPath)(Map.empty)

    response.status shouldBe Status.OK
    assertDeleteConfirmationPage(
      response,
      List(individualFile, twoRecordFile),
      deleteButtonText = "Delete 2 files"
    )
    TestRiskingStubs.verifyDeleteRiskingResultsFile(individualFile, count = 0)
    TestRiskingStubs.verifyDeleteRiskingResultsFile(twoRecordFile, count = 0)

  s"POST $deleteAllProcessedConfirmPath with no processed file should say so" in:
    TestRiskingStubs.stubListRiskingResultsFiles(Seq(entityFile))
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile))

    val response: WSResponse = post(deleteAllProcessedConfirmPath)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.selectOrFail(".govuk-error-summary__list").text() shouldBe "There are no processed files to delete."

  /** The page lists the files and posts exactly them to the delete action, which deletes them only then. */
  private def assertDeleteConfirmationPage(
    response: WSResponse,
    filenames: List[RiskingResultsFilename],
    deleteButtonText: String
  ): Unit =
    val page: Document = response.parseBodyAsJsoupDocument
    page.selectOrFail("h1").text() shouldBe "Delete processed risking results files"
    page.selectOrFail("#files-to-delete li").eachText().asScala.toList shouldBe filenames.map(_.value)
    val form: Element = page.selectOrFail(s"form[action=$deletePath]").selectOnlyOneElementOrFail()
    form.selectOrFail("input[type=hidden][name=filenames[]]").eachAttr("value").asScala.toList shouldBe filenames.map(_.value)
    form.selectOrFail("button").text() shouldBe deleteButtonText
    page.selectOrFail("a.govuk-link:contains(Cancel)").attr("href") shouldBe resultsFilesPath
    ()

  private def stubResultsFilesPage(): Unit =
    TestRiskingStubs.stubListRiskingResultsFiles(Seq(entityFile, twoRecordFile))
    TestRiskingStubs.stubUnprocessedRiskingResultsFiles(Seq(entityFile))
    ()

  private lazy val entityFile: RiskingResultsFilename = RiskingResultsFilename("test-only-entity-XARN0000001")
  private lazy val individualFile: RiskingResultsFilename = RiskingResultsFilename("test-only-individual-PERS0000001")
  private lazy val combinedFile: RiskingResultsFilename = TestRiskingResultsFilename.combined(frozenInstant)
  private lazy val twoRecordFile: RiskingResultsFilename = RiskingResultsFilename("test-only-combined-20261015T100000000Z")
  private lazy val largeFile: RiskingResultsFilename = RiskingResultsFilename("test-only-combined-20261015T110000000Z")

  private lazy val entityRecords: JsArray = Json.arr(Json.obj(
    "recordType" -> "Entity",
    "applicationReference" -> "XARN0000001",
    "failures" -> Json.arr()
  ))
  private lazy val individualRecords: JsArray = Json.arr(Json.obj(
    "recordType" -> "Individual",
    "personReference" -> "PERS0000001",
    "failures" -> Json.arr()
  ))
  private lazy val largeFileRecords: JsArray = JsArray((0 until 22).map(i =>
    Json.obj(
      "recordType" -> "Entity",
      "applicationReference" -> f"XARN$i%07d",
      "failures" -> Json.arr()
    )
  ))
