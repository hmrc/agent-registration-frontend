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

  def listSubmittedRiskingResultsFilenames()(using RequestHeader): Future[Set[String]] = testRiskingConnector.listSubmittedRiskingResultsFilenames()

  def viewRiskingResultsFile(filename: String)(using RequestHeader): Future[Option[String]] = testRiskingConnector.viewRiskingResultsFile(filename)

  def triggerRiskingResultsProcessing()(using RequestHeader): Future[Unit] = testRiskingConnector.triggerRiskingResultsProcessing()

  def findApplicationForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findApplicationForRisking(applicationReference)

  def findIndividualForRisking(
    personReference: PersonReference
  )(using RequestHeader): Future[Option[JsValue]] = testRiskingConnector.findIndividualForRisking(personReference)

  /** Builds a risking results file (matching what agent-registration-risking's SdesTestOnlyController expects: a JSON array of records) containing a single
    * Entity record for the selected failures and uploads it, simulating Minerva sending back results — independent of any individual's submission.
    */
  def submitEntityFailures(
    applicationReference: ApplicationReference,
    failures: Seq[EntityRiskingFailure]
  )(using RequestHeader): Future[Unit] =
    val record: JsValue = Json.obj(
      "recordType" -> "Entity",
      "applicationReference" -> applicationReference.value,
      "failures" -> failures.map(entityFailureJson)
    )
    val filename = s"test-only-${applicationReference.value}-${Instant.now().toEpochMilli}.json"
    testRiskingConnector.uploadRiskingResultsFile(filename, Json.arr(record))

  /** Same as `submitEntityFailures`, but for a single individual only — uploads just one Individual record, independent of the entity or any other individual
    * on the same application.
    */
  def submitIndividualFailures(
    personReference: PersonReference,
    failures: Seq[IndividualRiskingFailure]
  )(using RequestHeader): Future[Unit] =
    val record: JsValue = Json.obj(
      "recordType" -> "Individual",
      "personReference" -> personReference.value,
      "failures" -> failures.map(individualFailureJson)
    )
    val filename = s"test-only-${personReference.value}-${Instant.now().toEpochMilli}.json"
    testRiskingConnector.uploadRiskingResultsFile(filename, Json.arr(record))

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
