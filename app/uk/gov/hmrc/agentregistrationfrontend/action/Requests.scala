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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.ActionBuilder
import play.api.mvc.ActionRefiner
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.PresentIn
import uk.gov.hmrc.auth.core.retrieve.Credentials

object Requests:

  type ActionBuilder4[Data <: Tuple] = ActionBuilder[[X] =>> RequestWithData[X, Data], AnyContent]
  type ActionRefiner4[
    Data <: Tuple,
    NewData <: Tuple
  ] = ActionRefiner[[X] =>> RequestWithData[X, Data], [X] =>> RequestWithData[X, NewData]]

  type RequestWithData4[Data <: Tuple] = RequestWithData[AnyContent, Data]
  type DefaultRequest4[A] = RequestWithData4[DataEmpty]

  type DataWithAuth =
    (
      InternalUserId,
      GroupId,
      Credentials
    )
  type DataEmpty = EmptyTuple
  type DataWithApplication = AgentApplication *: DataWithAuth
  type RequestWithAuth4 = RequestWithData4[DataWithAuth]
  type RequestWithApplication4 = RequestWithData4[DataWithApplication]

  type DefaultRequest[A] = RequestWithData[A, DataEmpty]

  type AuthorisedRequest2[A] = RequestWithData[
    A,
    (
      InternalUserId,
      GroupId,
      Credentials
    )
  ]

  type AgentApplicationRequest2[A] = RequestWithData[
    A,
    (
      AgentApplication,
      InternalUserId,
      GroupId,
      Credentials
    )
  ]

  extension [
    A,
    Data <: Tuple
  ](r: RequestWithData[A, Data])

    inline def agentApplication(using AgentApplication PresentIn Data): AgentApplication = r.get
    inline def agentApplicationGeneralPartnership(using AgentApplicationGeneralPartnership PresentIn Data): AgentApplicationGeneralPartnership = r.get
    inline def agentApplicationLimitedCompany(using AgentApplicationLimitedCompany PresentIn Data): AgentApplicationLimitedCompany = r.get
    inline def agentApplicationLimitedPartnership(using AgentApplicationLimitedPartnership PresentIn Data): AgentApplicationLimitedPartnership = r.get
    inline def agentApplicationLlp(using AgentApplicationLlp PresentIn Data): AgentApplicationLlp = r.get
    inline def agentApplicationScottishLimitedPartnership(using
      AgentApplicationScottishLimitedPartnership PresentIn Data
    ): AgentApplicationScottishLimitedPartnership = r.get
    inline def agentApplicationScottishPartnership(using AgentApplicationScottishPartnership PresentIn Data): AgentApplicationScottishPartnership = r.get
    inline def agentApplicationSoleTrader(using AgentApplicationSoleTrader PresentIn Data): AgentApplicationSoleTrader = r.get

    inline def credentials(using Credentials PresentIn Data): Credentials = r.get
    inline def internalUserId(using InternalUserId PresentIn Data): InternalUserId = r.get
    inline def businessPartnerRecordResponse(using BusinessPartnerRecordResponse PresentIn Data): BusinessPartnerRecordResponse = r.get
    inline def maybeBusinessPartnerRecordResponse(using Option[BusinessPartnerRecordResponse] PresentIn Data): Option[BusinessPartnerRecordResponse] = r.get
    inline def agentType(using AgentType PresentIn Data): AgentType = r.get
