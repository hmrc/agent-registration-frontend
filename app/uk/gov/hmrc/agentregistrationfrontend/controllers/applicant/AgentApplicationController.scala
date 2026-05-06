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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedNonFixablePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.InProgressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ViewApplicationPage

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentApplicationController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  simplePage: SimplePage,
  confirmationPage: ConfirmationPage,
  inProgressPage: InProgressPage,
  failedNonFixablePage: FailedNonFixablePage,
  viewApplicationPage: ViewApplicationPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  // TODO: is this endpoint really needed?
  def landing: Action[AnyContent] = actions
    .getApplicationInProgress:
      implicit request =>
        // until we have more than the registration journey just go to the task list
        // which will redirect to the start of registration if needed
        Redirect(AppRoutes.apply.TaskListController.show)

  def applicationStatus: Action[AnyContent] = actions
    .getRiskingProgress:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        val submittedAt: Instant = agentApplication.getSubmittedAt
        val projectedDecisionDate: LocalDate = calculateDecisionDate(submittedAt)

        val riskingProgress: RiskingProgress = request.get
        riskingProgress match
          case RiskingProgress.ReadyForSubmission => // show the confirmation screen
            Ok(confirmationPage(
              dateOfDecision = displayDateForLang(Some(projectedDecisionDate)),
              agentApplication = agentApplication
            ))
          case RiskingProgress.SubmittedForRisking => // show the in progress screen
            val localDateSubmitted: LocalDate =
              submittedAt
                .atZone(ZoneId.systemDefault())
                .toLocalDate
            Ok(inProgressPage(
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
              agentApplication = agentApplication,
              dateOfDecision = displayDateForLang(Some(projectedDecisionDate)),
              dateSubmitted = displayDateForLang(Some(localDateSubmitted))
            ))
          case failedNonFixable: RiskingProgress.FailedNonFixable =>
            Ok(failedNonFixablePage(
              failedNonFixable = failedNonFixable,
              agentApplication = agentApplication,
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            ))
          case _: RiskingProgress.FailedFixable => // TODO: show the fixable failures page
            Ok(simplePage(
              h1 = "Fixable failure placeholder",
              bodyText = Some("Placeholder for the fixable failures...")
            ))
          case RiskingProgress.Approved =>
            Ok(simplePage(
              h1 = "RiskingProgress.Approved",
              bodyText = Some("Placeholder for the RiskingProgress.Approved case...")
            ))

  def viewSubmittedApplication: Action[AnyContent] = actions
    .getApplicationSubmitted:
      implicit request =>
        Ok(viewApplicationPage(
          entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
          agentApplication = request.get[AgentApplication]
        ))

  def startRegistration: Action[AnyContent] = actions.action:
    implicit request =>
      // if we use an endpoint like this, we can later change the flow without changing the URL
      Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show)

  def genericExitPage: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(simplePage(
        h1 = "You cannot use this service...",
        bodyText = Some(
          "Placeholder for the generic exit page..."
        )
      ))

  private def calculateDecisionDate(submittedAt: Instant): LocalDate =
    submittedAt
      .plus(appConfig.applicationDecisionLeadTime.toMillis, ChronoUnit.MILLIS)
      .atZone(ZoneId.systemDefault())
      .toLocalDate
