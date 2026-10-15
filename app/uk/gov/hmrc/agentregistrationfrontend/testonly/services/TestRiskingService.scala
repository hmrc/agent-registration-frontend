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
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestRiskingConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.EntityRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.IndividualRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestRiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UploadRiskingResultsFileOutcome

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class TestRiskingService @Inject() (
  testRiskingConnector: TestRiskingConnector
)(using ExecutionContext):

  import TestRiskingService.CombinedRiskingResultsFile

  def deleteAll()(using RequestHeader): Future[Unit] =
    for
      _ <- testRiskingConnector.deleteAllApplications()
      _ <- testRiskingConnector.deleteAllRiskingResultsFiles()
    yield ()

  def deleteAllRiskingResultsFiles()(using RequestHeader): Future[Unit] = testRiskingConnector.deleteAllRiskingResultsFiles()

  def runRisking()(using RequestHeader): Future[Unit] = testRiskingConnector.runRisking()

  def runResultsFileProcessing()(using RequestHeader): Future[Unit] = testRiskingConnector.runResultsFileProcessing()

  def viewNextRiskingFileContents()(using RequestHeader): Future[String] = testRiskingConnector.viewNextRiskingFileContents()

  def listSubmittedRiskingResultsFilenames()(using RequestHeader): Future[List[RiskingResultsFilename]] =
    testRiskingConnector.listSubmittedRiskingResultsFilenames()

  def getUnprocessedAvailableFiles()(using RequestHeader): Future[List[RiskingResultsFilename]] = testRiskingConnector.getUnprocessedAvailableFiles()

  def combineUnprocessedRiskingResultsFiles()(using RequestHeader): Future[Option[CombinedRiskingResultsFile]] =
    testRiskingConnector.getUnprocessedAvailableFiles().flatMap:
      case Nil => Future.successful(None)
      case unprocessedFilenames =>
        for
          fileContents <-
            Future.traverse(unprocessedFilenames): filename =>
              testRiskingConnector.viewRiskingResultsFile(filename).map: maybeContent =>
                filename -> maybeContent.getOrThrowExpectedDataMissing(
                  s"unprocessed risking results file '${filename.value}' to exist"
                )
          combinedFilename = TestRiskingResultsFilename.combined()
          combinedRecords = TestRiskingService.combineResultsJson(fileContents)
          _ <- testRiskingConnector.deleteAllRiskingResultsFiles()
          uploadOutcome <- testRiskingConnector.uploadRiskingResultsFile(combinedFilename, combinedRecords)
          _ =
            uploadOutcome match
              case UploadRiskingResultsFileOutcome.Uploaded => ()
              case UploadRiskingResultsFileOutcome.AlreadyExists =>
                throw new IllegalStateException(s"Combined risking results file already exists: ${combinedFilename.value}")
        yield Some(CombinedRiskingResultsFile(
          filename = combinedFilename,
          sourceFileCount = unprocessedFilenames.size,
          recordCount = combinedRecords.value.size
        ))

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

  final case class CombinedRiskingResultsFile(
    filename: RiskingResultsFilename,
    sourceFileCount: Int,
    recordCount: Int
  )

  private[testonly] def combineResultsJson(fileContents: Seq[(RiskingResultsFilename, String)]): JsArray = JsArray(fileContents.flatMap(parseRecords))

  private def parseRecords(file: (RiskingResultsFilename, String)): Seq[JsValue] =
    val (filename, content) = file
    Json.parse(content).validate[JsArray].fold(
      errors =>
        throw new IllegalArgumentException(
          s"Risking results file '${filename.value}' did not contain a JSON array: ${Json.stringify(JsError.toJson(errors))}"
        ),
      _.value.toIndexedSeq
    )
