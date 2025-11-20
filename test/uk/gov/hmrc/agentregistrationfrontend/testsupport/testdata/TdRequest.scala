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

import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withAuthTokenInSession
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withDeviceId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withRequestId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withTrueClientIp
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withTrueClientPort
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.withAgentApplicationId

trait TdRequest {
  dependencies: TdBase =>

  def authToken: String = "authorization-value-123"
  def akamaiReputationValue: String = "akamai-reputation-value-123"
  def requestId: String = "request-id-value-123"
  def trueClientIp: String = "client-ip-123"
  def trueClientPort: String = "client-port-123"
  def deviceIdInRequest: String = "device-id-123"

  def baseRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withRequestId()
    .withTrueClientIp()
    .withTrueClientPort()
    .withDeviceId()

  def requestNotLoggedIn: Request[AnyContentAsEmpty.type] = baseRequest
  def requestLoggedIn: Request[AnyContentAsEmpty.type] = baseRequest.withAuthTokenInSession()

  def requestLoggedInWithAgentApplicationId: Request[AnyContentAsEmpty.type] = baseRequest
    .withAuthTokenInSession()
    .withAgentApplicationId(agentApplicationId.value)

  def individualAuthorisedRequestLoggedInWithAgentApplicationId: IndividualAuthorisedRequest[AnyContentAsEmpty.type] =
    new IndividualAuthorisedRequest(
      internalUserId = internalUserId,
      request = requestLoggedInWithAgentApplicationId,
      credentials = credentials,
      nino = Some(nino),
      saUtr = Some(saUtr)
    )
  def individualAuthorisedRequestLoggedInWithOutAgentApplicationId: IndividualAuthorisedRequest[AnyContentAsEmpty.type] =
    new IndividualAuthorisedRequest(
      internalUserId = internalUserId,
      request = requestNotLoggedIn,
      credentials = credentials,
      nino = Some(nino),
      saUtr = Some(saUtr)
    )

}
