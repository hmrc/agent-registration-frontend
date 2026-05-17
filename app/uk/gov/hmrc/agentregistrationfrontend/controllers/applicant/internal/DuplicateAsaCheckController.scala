/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistrationfrontend.services.SubscriptionService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import com.softwaremill.quicklens.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class DuplicateAsaCheckController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  subscriptionService: SubscriptionService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
    .ensure(
      condition = _.agentApplication.isDuplicateAsaCheckRequired,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Duplicate ASA verification already done. Redirecting to next check.")
          Redirect(nextCheckEndpoint)
    )
    .async:
      implicit request =>
        request.get[BusinessPartnerRecordResponse] match
          case bpr if bpr.isAlreadyRegisteredAsAgent => handleRegisteredAsAgent(bpr.getAgentReferenceNumber)
          case _ => Future.successful(Redirect(nextCheckEndpoint))

  private def handleRegisteredAsAgent(
    agentReferenceNumber: Arn
  )(using request: RequestWithData[DataWithApplicationAndBpr]): Future[Result] =
    for
      arnHasPrincipalGroups: Boolean <- enrolmentStoreProxyConnector.queryArnHasPrincipleGroups(agentReferenceNumber)
      _ <-
        if arnHasPrincipalGroups
        then Future.unit
        else subscriptionService.addKnownFactsAndEnrolUk(agentReferenceNumber)
      _ <- agentApplicationService.upsert(
        request.agentApplication
          .modify(_.isDuplicateAsa)
          .setTo(Some(arnHasPrincipalGroups))
      )
    yield
      if arnHasPrincipalGroups then Redirect(alreadySubscribedPage)
      else Redirect(nextCheckEndpoint)

  private def alreadySubscribedPage: Call = AppRoutes.apply.checkfailed.AlreadySubscribedController.show
  private def nextCheckEndpoint: Call = AppRoutes.apply.internal.UnifiedCustomerRegistryController.populateApplicationIdentifiersFromUcr

  extension (agentApplication: AgentApplication)
    private def isDuplicateAsaCheckRequired: Boolean = agentApplication.isDuplicateAsa.isEmpty
