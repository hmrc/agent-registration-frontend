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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestRiskingConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.EntityRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.IndividualRiskingFailure
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.TestRiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.UploadRiskingResultsFileOutcome

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class TestRiskingService @Inject() (
  testRiskingConnector: TestRiskingConnector
):

  def deleteAll()(using RequestHeader): Future[Unit] = testRiskingConnector.deleteAllApplications()

  def runRisking()(using RequestHeader): Future[Unit] = testRiskingConnector.runRisking()

  def runResultsFileProcessing()(using RequestHeader): Future[Unit] = testRiskingConnector.runResultsFileProcessing()

  def viewNextRiskingFileContents()(using RequestHeader): Future[String] = testRiskingConnector.viewNextRiskingFileContents()

  def listSubmittedRiskingResultsFilenames()(using RequestHeader): Future[Set[String]] = testRiskingConnector.listSubmittedRiskingResultsFilenames()

  def viewRiskingResultsFile(filename: String)(using RequestHeader): Future[Option[String]] = testRiskingConnector.viewRiskingResultsFile(filename)

  def findApplicationForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findApplicationForRisking(applicationReference)

  def findIndividualsForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findIndividualsForRisking(applicationReference)

  def findIndividualForRisking(
    personReference: PersonReference
  )(using RequestHeader): Future[Option[JsValue]] = testRiskingConnector.findIndividualForRisking(personReference)

  def findCompletedRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findCompletedRisking(applicationReference)

  def submitEntityFailures(
    applicationReference: ApplicationReference,
    failures: Seq[EntityRiskingFailure],
    reSubmittedAt: Option[Instant]
  )(using RequestHeader): Future[UploadRiskingResultsFileOutcome] =
    val record = Json.obj(
      "recordType" -> "Entity",
      "applicationReference" -> applicationReference.value,
      "failures" -> failures.map(entityFailureJson)
    )
    testRiskingConnector.uploadRiskingResultsFile(TestRiskingResultsFilename.entity(applicationReference, reSubmittedAt), Json.arr(record))

  def submitIndividualFailures(
    personReference: PersonReference,
    failures: Seq[IndividualRiskingFailure],
    reSubmittedAt: Option[Instant]
  )(using RequestHeader): Future[UploadRiskingResultsFileOutcome] =
    val record = Json.obj(
      "recordType" -> "Individual",
      "personReference" -> personReference.value,
      "failures" -> failures.map(individualFailureJson)
    )
    testRiskingConnector.uploadRiskingResultsFile(TestRiskingResultsFilename.individual(personReference, reSubmittedAt), Json.arr(record))

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
