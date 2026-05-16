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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.groupId
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.internalUserId
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithApplicationAndBpr
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithAuth
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.KnownFact
import uk.gov.hmrc.agentregistrationfrontend.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.Errors.getOrThrowExpectedDataMissing

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject() (taxEnrolmentsConnector: TaxEnrolmentsConnector)(using ec: ExecutionContext):

  def addKnownFactsAndEnrolUk(
    arn: Arn
  )(using request: RequestWithData[DataWithApplicationAndBpr]): Future[Unit] = {
    val bpr = request.get[BusinessPartnerRecordResponse]
    addKnownFactsAndEnrol(
      arn = arn,
      knownFactKey = "AgencyPostcode",
      knownFactValue = bpr.address.postalCode.getOrThrowExpectedDataMissing(
        "BPR postcode missing when trying to update AgencyPostcode known fact"
      ),
      friendlyName = bpr.getEntityName,
      userId = request.internalUserId,
      groupId = request.groupId
    )
  }

  private def addKnownFactsAndEnrol(
    arn: Arn,
    knownFactKey: String,
    knownFactValue: String,
    friendlyName: String,
    userId: InternalUserId,
    groupId: GroupId
  )(implicit rh: RequestHeader): Future[Unit] =
    val enrolRequest = EnrolmentRequest(
      userId = userId.value,
      `type` = "principal",
      friendlyName = friendlyName,
      Seq(KnownFact(knownFactKey, knownFactValue))
    )
    for
      _ <- taxEnrolmentsConnector.addKnownFacts(
        arn.value,
        knownFactKey,
        knownFactValue
      )
      _ <- taxEnrolmentsConnector.enrol(
        groupId.value,
        arn,
        enrolRequest
      )
    yield ()
