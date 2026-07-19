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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistrationfrontend.testonly.connectors.TestRiskingConnector

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class TestRiskingService @Inject() (
  testRiskingConnector: TestRiskingConnector
):

  def deleteAll()(using RequestHeader): Future[Unit] = testRiskingConnector.deleteAllApplications()

  def runRisking()(using RequestHeader): Future[Unit] = testRiskingConnector.runRisking()

  def viewNextRiskingFileContents()(using RequestHeader): Future[String] = testRiskingConnector.viewNextRiskingFileContents()

  def findApplicationForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findApplicationForRisking(applicationReference)

  def findIndividualsForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findIndividualsForRisking(applicationReference)

  def findCompletedRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    testRiskingConnector.findCompletedRisking(applicationReference)
