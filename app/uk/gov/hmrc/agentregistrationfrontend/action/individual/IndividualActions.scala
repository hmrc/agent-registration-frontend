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

package uk.gov.hmrc.agentregistrationfrontend.action.individual

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineFutureEither
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object IndividualActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

  type DataWithAdditionalIdentifiers = Option[Nino] *: Option[SaUtr] *: DataWithAuth
  type RequestWithAdditionalIdentifiers = RequestWithData[DataWithAdditionalIdentifiers]
  type RequestWithAdditionalIdentifiersCt[ContentType] = RequestWithDataCt[ContentType, DataWithAdditionalIdentifiers]

@Singleton
class IndividualActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  individualAuthorisedRefiner: IndividualAuthRefiner
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export IndividualActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAuth)

  val authorisedWithAdditionalIdentifiers: ActionBuilderWithData[DataWithAdditionalIdentifiers] = action
    .refineFutureEither(individualAuthorisedRefiner.refineIntoRequestWithAdditionalIdentifiers)
