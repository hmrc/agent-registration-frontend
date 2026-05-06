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

package uk.gov.hmrc.agentregistrationfrontend.action.applicant

import play.api.mvc.*
import play.api.mvc.Results.Redirect
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
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineFutureEither
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.audit.AuditService
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ApplicantActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithAuth = (InternalUserId, GroupId, Credentials)
  type RequestWithAuth = RequestWithData[DataWithAuth]
  type RequestWithAuthCt[ContentType] = RequestWithDataCt[ContentType, DataWithAuth]

  type DataWithApplication = AgentApplication *: DataWithAuth
  type RequestWithApplication = RequestWithData[DataWithApplication]
  type RequestWithApplicationCt[A] = RequestWithDataCt[A, DataWithApplication]

  type DataWithApplicationAndBpr = BusinessPartnerRecordResponse *: DataWithApplication
  type RequestWithApplicationAndBpr = RequestWithData[DataWithApplicationAndBpr]
  type RequestWithApplicationAndBprCt[A] = RequestWithDataCt[A, DataWithApplicationAndBpr]

  type DataWithRiskingProgress = RiskingProgress *: DataWithApplicationAndBpr
  type RequestWithRiskingProgress = RequestWithData[DataWithRiskingProgress]
  type RequestWithRiskingProgressCt[A] = RequestWithDataCt[A, DataWithRiskingProgress]

@Singleton
class ApplicantActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  authorisedActionRefiner: ApplicantAuthRefiner,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService,
  agentRegistrationRiskingService: AgentRegistrationRiskingService,
  auditService: AuditService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export ApplicantActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineFutureEither(authorisedActionRefiner.refine)

  val getApplication: ActionBuilderWithData[DataWithApplication] = authorised
    .refine:
      implicit request: RequestWithData[DataWithAuth] =>
        agentApplicationService
          .find()
          .map[Result | RequestWithApplication]:
            case Some(agentApplication) => request.add(agentApplication)
            case None =>
              val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
              logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.get[InternalUserId].value}. Redirecting to startRegistration page ($redirect)")
              Redirect(redirect)
    .refine:
      implicit request: RequestWithApplication =>
        val aa: AgentApplication = request.agentApplication
        if aa.continueJourney()
        then
          auditService.auditContinueApplication(aa)
          aa.updateCachedSessionId()
        else request

  def getApplicationInProgress: ActionBuilderWithData[DataWithApplication] = getApplication
    .ensure(
      condition = _.get[AgentApplication].isInProgress,
      resultWhenConditionNotMet =
        implicit request =>
          val call = AppRoutes.apply.AgentApplicationController.applicationStatus
          logger.warn(
            s"The application is not in progress" +
              s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )
          Redirect(call.url)
    )

  val getApplicationSubmitted: ActionBuilderWithData[DataWithApplicationAndBpr] =
    getApplication
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
      .getBusinessPartnerRecord

  val getRiskingProgress: ActionBuilderWithData[DataWithRiskingProgress] = getApplicationSubmitted
    .refine(implicit request =>
      agentRegistrationRiskingService
        .getRiskingProgress(request.agentApplication.applicationReference)
        .map: riskingProgress =>
          request.add[RiskingProgress](riskingProgress)
    )

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def getBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      BusinessPartnerRecordResponse AbsentIn Data
    ): ActionBuilderWithData[BusinessPartnerRecordResponse *: Data] = ab.refine:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(_.getOrThrowExpectedDataMissing(s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"))
          .map(request.add)

    inline def getMaybeBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      Option[BusinessPartnerRecordResponse] AbsentIn Data
    ): ActionBuilderWithData[Option[BusinessPartnerRecordResponse] *: Data] = ab.refine:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(request.add)

  extension (agentApplication: AgentApplication)

    private def continueJourney()(using request: RequestWithApplication): Boolean =
      val cachedSessionId = SessionId.make(hc.sessionId.getOrThrowExpectedDataMissing("sessionId"))
      agentApplication.cachedSessionId =!= cachedSessionId

    private def updateCachedSessionId()(using request: RequestWithApplication): Future[RequestWithApplication] =
      val cachedSessionId = SessionId.make(hc.sessionId.getOrThrowExpectedDataMissing("sessionId"))
      val updatedApplication: AgentApplication =
        agentApplication match
          case a: AgentApplicationLlp => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationSoleTrader => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationLimitedCompany => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationGeneralPartnership => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationLimitedPartnership => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationScottishLimitedPartnership => a.copy(cachedSessionId = cachedSessionId)
          case a: AgentApplicationScottishPartnership => a.copy(cachedSessionId = cachedSessionId)

      agentApplicationService.upsert(updatedApplication).map(_ =>
        request.update(updatedApplication)
      )
