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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.StartPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.OutcomeStartPage

import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartController @Inject (
  appConfig: AppConfig
)(
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  startPage: StartPage,
  outcomeStartPage: OutcomeStartPage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def start(linkId: LinkId): Action[AnyContent] = actions
    .action
    .async:
      implicit request =>
        applicationService.find(linkId).map:
          case Some(agentApplication) if agentApplication.isAfterSentForRisking =>
            if appConfig.Features.fixableFailures
            then
              agentApplication.applicationState match
                case ApplicationState.RiskingCompleted =>
                  Ok(outcomeStartPage(
                    linkId: LinkId
                  ))
                case _ => Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId)) // this case will only ever be for first time submissions, RiskingCompleted is the status for resubmissions
            else
              Redirect(AppRoutes.providedetails.riskingprogress.RiskingProgressController.show(linkId)) // the 'old world' where we call risking for progress
          case Some(agentApplication) =>
            val applicationExpiryInstant: Instant = agentApplication
              .applicationExpiresAt
              .getOrThrowExpectedDataMissing("Application expiry date is missing from application in progress")
            Ok(startPage(
              linkId = linkId,
              agentApplicationExpiryDate = applicationExpiryInstant.atZone(ZoneId.systemDefault()).toLocalDate
            ))
          case None =>
            logger.info(s"Application for linkId $linkId not found")
            NotFound
