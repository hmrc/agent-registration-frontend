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
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestRiskingConnector
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingFailureOption

import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class TestRiskingService @Inject() (
  testRiskingConnector: TestRiskingConnector
):

  def deleteAll()(using RequestHeader): Future[Unit] = testRiskingConnector.deleteAllApplications()

  /** Builds a risking results file (matching what agent-registration-risking's SdesTestOnlyController expects: a JSON array of records, one per
    * entity/individual) from the selected failure codes and uploads it, simulating Minerva sending back results.
    */
  def submitFailures(
    applicationReference: ApplicationReference,
    entityFailureCodes: Seq[String],
    individualFailureCodesByPersonReference: Map[PersonReference, Seq[String]]
  )(using RequestHeader): Future[Unit] =
    val entityRecord: JsValue = Json.obj(
      "recordType" -> "Entity",
      "applicationReference" -> applicationReference.value,
      "failures" -> entityFailureCodes.flatMap(RiskingFailureOption.find).map(failureJson)
    )
    val individualRecords: Seq[JsValue] = individualFailureCodesByPersonReference.toSeq.map { case (personReference, codes) =>
      Json.obj(
        "recordType" -> "Individual",
        "personReference" -> personReference.value,
        "failures" -> codes.flatMap(RiskingFailureOption.find).map(failureJson)
      )
    }
    val content: JsArray = JsArray(entityRecord +: individualRecords)
    val filename = s"test-only-${applicationReference.value}-${Instant.now().toEpochMilli}.json"
    testRiskingConnector.uploadRiskingResultsFile(filename, content)

  private def failureJson(option: RiskingFailureOption): JsValue = Json.obj(
    "reasonCode" -> option.reasonCode,
    "reasonDescription" -> option.reasonDescription,
    "checkId" -> option.checkId,
    "checkDescription" -> option.checkDescription
  )
