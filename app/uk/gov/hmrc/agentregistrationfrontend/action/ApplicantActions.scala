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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.AuthorisedActionRefiner
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object ApplicantActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, GroupId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

  type DataWithApplication = AgentApplication *: DataWithAuth
  type RequestWithApplication = RequestWithData[DataWithApplication]
  type RequestWithApplicationCt[A] = RequestWithDataCt[A, DataWithApplication]

@Singleton
class ApplicantActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  authorisedActionRefiner: AuthorisedActionRefiner,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionsHelper.*
  export ApplicantActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refine2(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineAsync(authorisedActionRefiner.refine)

  val getApplication: ActionBuilderWithData[DataWithApplication] = authorised
    .refine4:
      implicit request: RequestWithData[DataWithAuth] =>
        agentApplicationService
          .find()
          .map[Result | RequestWithApplication]:
            case Some(agentApplication) => request.add(agentApplication)
            case None =>
              val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
              logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.get[InternalUserId].value}. Redirecting to startRegistration page ($redirect)")
              Redirect(redirect)

  def getApplicationInProgress: ActionBuilderWithData[DataWithApplication] = getApplication
    .ensure(
      condition = _.get[AgentApplication].isInProgress,
      resultWhenConditionNotMet =
        implicit request =>
          // TODO: this is a temporary solution and should be revisited once we have full journey implemented
          val call = AppRoutes.apply.AgentApplicationController.applicationSubmitted
          logger.warn(
            s"The application is not in the final state" +
              s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )
          Redirect(call.url)
    )

  val getApplicationSubmitted: ActionBuilderWithData[DataWithApplication] = getApplication
    .ensure(
      condition = _.agentApplication.hasFinished,
      resultWhenConditionNotMet =
        implicit request =>
          // TODO: this is a temporary solution and should be revisited once we have full journey implemented
          val call = AppRoutes.apply.AgentApplicationController.landing // or task list
          logger.warn(
            s"The application is not in the final state" +
              s" (current application state: ${request.agentApplication.applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )
          Redirect(call.url)
    )

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def getBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      BusinessPartnerRecordResponse AbsentIn Data
    ): ActionBuilderWithData[BusinessPartnerRecordResponse *: Data] = ab.refine4:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(_.getOrThrowExpectedDataMissing(s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"))
          .map(request.add)

    inline def getMaybeBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      Option[BusinessPartnerRecordResponse] AbsentIn Data
    ): ActionBuilderWithData[Option[BusinessPartnerRecordResponse] *: Data] = ab.refine4:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(request.add)
