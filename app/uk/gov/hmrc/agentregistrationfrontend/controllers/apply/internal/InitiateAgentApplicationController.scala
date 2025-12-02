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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class InitiateAgentApplicationController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  agentApplicationService: AgentApplicationService,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
  appConfig: AppConfig,
  applicationFactory: ApplicationFactory
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by Government Gateway upon successful login.
    */
  def initiateAgentApplication(
    agentType: AgentType,
    businessType: BusinessType
  ): Action[AnyContent] = actions
    .Applicant
    .authorised
    .ensureAsync(
      condition =
        implicit request =>
          val isHmrcAsAgentEnrolmentAllocatedToGroup: Future[Boolean] = enrolmentStoreProxyConnector
            .queryEnrolmentsAllocatedToGroup(request.groupId)
            .map(!_.exists(e => e.service === appConfig.hmrcAsAgentEnrolment.key && e.state === "Activated"))
          isHmrcAsAgentEnrolmentAllocatedToGroup
      ,
      resultWhenConditionNotMet =
        implicit request =>
          val redirectUrl: String = appConfig.taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment
          logger.info(s"No Application can be created. ${appConfig.hmrcAsAgentEnrolment} is already assigned to group ${request.groupId}. Redirecting to taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment ($redirectUrl)")
          Future.successful(Redirect(redirectUrl))
    )
    .async:
      implicit request =>
        if agentType =!= AgentType.UkTaxAgent then Errors.notImplemented("only UkTaxAgent is supported for now") else ()
        if businessType =!= BusinessType.Partnership.LimitedLiabilityPartnership then Errors.notImplemented("only LLP is supported for now") else ()

        val nextEndpoint: Call = routes.GrsController.startJourney()

        agentApplicationService.find().flatMap:
          case Some(agentApplication) =>
            logger.info("Application already exists, redirecting to task list")
            Future.successful(Redirect(nextEndpoint))
          case None =>
            logger.info(s"Application does not exist, creating new application: $agentType, $businessType")
            agentApplicationService
              .upsert(applicationFactory.makeNewAgentApplicationLlp(request.internalUserId, request.groupId))
              .map(_ => Redirect(nextEndpoint))
