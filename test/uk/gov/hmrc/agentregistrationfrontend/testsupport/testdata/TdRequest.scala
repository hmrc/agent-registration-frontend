/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.EmptyData
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.http.HeaderCarrier

trait TdRequest {
  dependencies: TdBase =>

  import TdSupport.*

  def authToken: String = "authorization-value-123"
  def akamaiReputationValue: String = "akamai-reputation-value-123"
  def requestId: String = "request-id-value-123"
  def trueClientIp: String = "client-ip-123"
  def trueClientPort: String = "client-port-123"
  def deviceIdInRequest: String = "device-id-123"

  def baseRequest: FakeRequest[AnyContent] = FakeRequest()
    .withRequestId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  def headerCarrier: HeaderCarrier = HeaderCarrier()

  def rawRequestNotLoggedIn: Request[AnyContent] = baseRequest
  def rawRequestLoggedIn: Request[AnyContent] = baseRequest.withAuthTokenInSession()

  def requestNotLoggedIn: RequestWithData[EmptyData] = RequestWithDataCt.empty(rawRequestNotLoggedIn)
  def requestLoggedIn: RequestWithData[EmptyData] = RequestWithDataCt.empty(rawRequestLoggedIn)

  def deleteMerequestLoggedIn: Request[AnyContent] = baseRequest.withAuthTokenInSession()

  object ApplicantRequests:

    import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.*

    def requestWithAuthData: RequestWithData[DataWithAuth] = RequestWithDataCt.apply(
      rawRequestLoggedIn,
      (
        dependencies.internalUserId,
        dependencies.groupId,
        dependencies.credentials
      )
    )

  object IndividualRequests:

    import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions.*

    def requestWithAuthData: RequestWithData[DataWithAuth] = RequestWithDataCt.apply(
      rawRequestLoggedIn,
      (
        dependencies.internalUserId,
        dependencies.credentials
      )
    )

    def requestWithAdditionalIdentifiers(
      maybeBino: Option[Nino] = Some(dependencies.nino),
      maybeSaUtr: Option[SaUtr] = Some(dependencies.saUtr)
    ): RequestWithData[DataWithAdditionalIdentifiers] = RequestWithDataCt.apply(
      rawRequestLoggedIn,
      (
        maybeBino,
        maybeSaUtr,
        dependencies.internalUserId,
        dependencies.credentials
      )
    )

    def requestWithIndividualProvidedDetails: RequestWithData[DataWithIndividualProvidedDetailsToBeDeleted] = RequestWithDataCt.apply(
      rawRequestLoggedIn,
      (
        dependencies.individualProvidedDetailsToBeDeleted,
        dependencies.internalUserId,
        dependencies.credentials
      )
    )

}
