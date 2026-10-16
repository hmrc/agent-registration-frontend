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

package uk.gov.hmrc.agentregistrationfrontend.testonly.services

import play.api.libs.json.JsArray
import play.api.libs.json.JsError
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesRequest
import uk.gov.hmrc.agentregistration.shared.testonly.FindRiskingResultsFilesResponse
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestRiskingConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.EntityRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.IndividualRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsRecord
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestRiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UploadRiskingResultsFileOutcome

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class TestRiskingService @Inject() (
  testRiskingConnector: TestRiskingConnector,
  clock: Clock
)(using ExecutionContext):

  def deleteAll()(using RequestHeader): Future[Unit] =
    for
      _ <- testRiskingConnector.deleteAllApplications()
      _ <- testRiskingConnector.deleteAllRiskingResultsFiles()
    yield ()

  def runRisking()(using RequestHeader): Future[Unit] = testRiskingConnector.runRisking()

  def runResultsFileProcessing()(using RequestHeader): Future[Unit] = testRiskingConnector.runResultsFileProcessing()

  def viewNextRiskingFileContents()(using RequestHeader): Future[String] = testRiskingConnector.viewNextRiskingFileContents()

  def listSubmittedRiskingResultsFilenames()(using RequestHeader): Future[List[RiskingResultsFilename]] =
    testRiskingConnector.listSubmittedRiskingResultsFilenames()

  def getUnprocessedAvailableFiles()(using RequestHeader): Future[List[RiskingResultsFilename]] = testRiskingConnector.getUnprocessedAvailableFiles()

  def findRiskingResultsFilesByReference(
    applicationReferences: Seq[ApplicationReference],
    personReferences: Seq[PersonReference]
  )(using RequestHeader): Future[FindRiskingResultsFilesResponse] = testRiskingConnector.findRiskingResultsFiles(FindRiskingResultsFilesRequest(
    applicationReferences = applicationReferences.toList,
    personReferences = personReferences.toList
  ))

  def getRiskingResultsFileRecords(filename: RiskingResultsFilename)(using RequestHeader): Future[Option[Seq[RiskingResultsRecord]]] = testRiskingConnector
    .viewRiskingResultsFile(filename)
    .map(_.map(content => TestRiskingService.parseRecordReferences(filename, content)))

  /** The submitted files that have been processed, so deleting them loses no results. */
  def listProcessedRiskingResultsFilenames()(using RequestHeader): Future[List[RiskingResultsFilename]] =
    for
      riskingResultsFilenames <- listSubmittedRiskingResultsFilenames()
      unprocessedRiskingResultsFilenames <- getUnprocessedAvailableFiles()
    yield riskingResultsFilenames.filterNot(unprocessedRiskingResultsFilenames.contains)

  def deleteRiskingResultsFiles(filenames: Seq[RiskingResultsFilename])(using RequestHeader): Future[Unit] = Future.traverse(filenames)(filename =>
    testRiskingConnector.deleteRiskingResultsFile(filename)
  ).map(_ => ())

  /** Uploads the records of `filenames` as one new file, then deletes those files so their records are not processed twice. */
  def combineRiskingResultsFiles(filenames: Seq[RiskingResultsFilename])(using RequestHeader): Future[Unit] =
    for
      fileContents <-
        Future.traverse(filenames): filename =>
          testRiskingConnector.viewRiskingResultsFile(filename).map: maybeContent =>
            filename -> maybeContent.getOrThrowExpectedDataMissing(s"risking results file '${filename.value}' to exist")
      combinedFilename = TestRiskingResultsFilename.combined(Instant.now(clock))
      uploadOutcome <- testRiskingConnector.uploadRiskingResultsFile(combinedFilename, TestRiskingService.combineResultsJson(fileContents))
      _ =
        uploadOutcome match
          case UploadRiskingResultsFileOutcome.Uploaded => ()
          case UploadRiskingResultsFileOutcome.AlreadyExists =>
            throw new IllegalStateException(s"Combined risking results file already exists: ${combinedFilename.value}")
      _ <- deleteRiskingResultsFiles(filenames)
    yield ()

  def viewRiskingResultsFile(
    filename: RiskingResultsFilename
  )(using RequestHeader): Future[Option[String]] = testRiskingConnector.viewRiskingResultsFile(filename)

  def findApplicationForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findApplicationForRisking(applicationReference)

  def findIndividualForRisking(
    personReference: PersonReference
  )(using RequestHeader): Future[Option[JsValue]] = testRiskingConnector.findIndividualForRisking(personReference)

  def findCompletedRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findCompletedRisking(applicationReference)

  def submitEntityFailures(
    applicationReference: ApplicationReference,
    failures: Seq[EntityRiskingFailure],
    fileName: RiskingResultsFilename
  )(using RequestHeader): Future[UploadRiskingResultsFileOutcome] =
    val record = Json.obj(
      "recordType" -> "Entity",
      "applicationReference" -> applicationReference.value,
      "failures" -> failures.map(entityFailureJson)
    )
    testRiskingConnector.uploadRiskingResultsFile(fileName, Json.arr(record))

  def submitIndividualFailures(
    personReference: PersonReference,
    failures: Seq[IndividualRiskingFailure],
    fileName: RiskingResultsFilename
  )(using RequestHeader): Future[UploadRiskingResultsFileOutcome] =
    val record = Json.obj(
      "recordType" -> "Individual",
      "personReference" -> personReference.value,
      "failures" -> failures.map(individualFailureJson)
    )
    testRiskingConnector.uploadRiskingResultsFile(fileName, Json.arr(record))

  private def entityFailureJson(failure: EntityRiskingFailure): JsValue = Json.obj(
    "reasonCode" -> failure.reasonCode,
    "reasonDescription" -> failure.reasonDescription,
    "checkId" -> failure.checkId,
    "checkDescription" -> failure.checkDescription
  )

  private def individualFailureJson(failure: IndividualRiskingFailure): JsValue = Json.obj(
    "reasonCode" -> failure.reasonCode,
    "reasonDescription" -> failure.reasonDescription,
    "checkId" -> failure.checkId,
    "checkDescription" -> failure.checkDescription
  )

object TestRiskingService:

  private[testonly] def combineResultsJson(fileContents: Seq[(RiskingResultsFilename, String)]): JsArray = JsArray(fileContents.flatMap(parseRecords))

  private[testonly] def parseRecordReferences(
    filename: RiskingResultsFilename,
    content: String
  ): Seq[RiskingResultsRecord] = parseRecords((filename, content)).map: record =>
    (record \ "recordType").as[String] match
      case "Entity" => RiskingResultsRecord.ForApplication(ApplicationReference((record \ "applicationReference").as[String]))
      case "Individual" => RiskingResultsRecord.ForPerson(PersonReference((record \ "personReference").as[String]))
      case recordType => throw new IllegalArgumentException(s"Risking results file '${filename.value}' has a record of unknown type '$recordType'")

  private def parseRecords(file: (RiskingResultsFilename, String)): Seq[JsValue] =
    val (filename, content) = file
    Json.parse(content).validate[JsArray].fold(
      errors =>
        throw new IllegalArgumentException(
          s"Risking results file '${filename.value}' did not contain a JSON array: ${Json.stringify(JsError.toJson(errors))}"
        ),
      _.value.toIndexedSeq
    )
