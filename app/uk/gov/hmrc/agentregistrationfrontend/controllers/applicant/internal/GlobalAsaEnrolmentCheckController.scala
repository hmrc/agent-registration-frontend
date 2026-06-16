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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class GlobalAsaEnrolmentCheckController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  /** Check if the BPR associated with the UTR of the user is already subscribed as an agent.
    *
    * If the BPR is already subscribed as an agent we call ES1 to check if that enrolment has any user groups. If it has at least one user group we can be
    * satisfied that the enrolment is active but the group of the currently logged-in user does not have that assignment so the user fails this check and gets
    * sent to the agent-already-subscribed page, they would need to go to an admin to get that assignment added to their group.
    *
    * If the enrolment has no groups associated with it, we allow the user to continue, they will be risked, and then we can add the enrolment to their group
    * (assuming they successfully return from minerva.)
    */
  def check(): Action[AnyContent] = actions
    .getApplicationInProgress
    .ensure(
      condition =
        _.agentApplication
          .applicationState === ApplicationState.GrsDataReceived,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Missing data from GRS, redirecting to start GRS registration")
          Redirect(AppRoutes.apply.AgentApplicationController.startRegistration)
    )
    .ensure(
      condition = _.agentApplication.isGlobalAsaEnrolmentCheckRequired,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Global ASA enrolment check already done. Redirecting to next endpoint.")
          Redirect(nextEndpoint)
    )
    .getBusinessPartnerRecord
    .async:
      implicit request =>
        request.get[BusinessPartnerRecordResponse] match
          case bpr if bpr.isAlreadyRegisteredAsAgent => handleRegisteredAsAgent(bpr.getAgentReferenceNumber)
          case _ =>
            updateGlobalAsaEnrolmentCheckResult(globalAsaEnrolmentCheckResult = CheckResult.Pass)
              .map(_ => Redirect(nextEndpoint))

  private def handleRegisteredAsAgent(
    agentReferenceNumber: Arn
  )(using request: RequestWithData[DataWithApplicationAndBpr]): Future[Result] =
    for
      arnHasPrincipalGroups: Boolean <- enrolmentStoreProxyConnector.queryArnHasPrincipalGroups(agentReferenceNumber)
      result <-
        if arnHasPrincipalGroups
        then
          updateGlobalAsaEnrolmentCheckResult(globalAsaEnrolmentCheckResult = CheckResult.Fail).map: _ =>
            Redirect(alreadySubscribedPage)
        else
          updateGlobalAsaEnrolmentCheckResult(globalAsaEnrolmentCheckResult = CheckResult.Pass).map: _ =>
            Redirect(nextEndpoint)
    yield result

  private def updateGlobalAsaEnrolmentCheckResult(
    globalAsaEnrolmentCheckResult: CheckResult
  )(using request: RequestWithData[DataWithApplicationAndBpr]): Future[Unit] = agentApplicationService.upsert(
    request.agentApplication
      .modify(_.globalAsaEnrolmentCheckResult)
      .setTo(Some(globalAsaEnrolmentCheckResult))
  )

  private def alreadySubscribedPage: Call = AppRoutes.apply.checkfailed.AgentAlreadySubscribedController.show
  private def nextEndpoint: Call = AppRoutes.apply.TaskListController.show

  extension (agentApplication: AgentApplication)
    private def isGlobalAsaEnrolmentCheckRequired: Boolean = agentApplication.globalAsaEnrolmentCheckResult =!= Some(CheckResult.Pass)
