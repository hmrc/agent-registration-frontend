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
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedFixableStartPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.FailedNonFixablePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.InProgressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ViewApplicationPage

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.annotation.nowarn

@Singleton
class AgentApplicationController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  simplePage: SimplePage,
  confirmationPage: ConfirmationPage,
  inProgressPage: InProgressPage,
  failedNonFixablePage: FailedNonFixablePage,
  failedFixableStartPage: FailedFixableStartPage,
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

  /** When we are abstracting combined cases as we do in isInProgress(), incorrect warnings about non-exhaustive pattern matching appear. So suppressing this
    * warning until we no longer need the fixable failures feature flag
    */
  @nowarn
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
          case rp if isInProgress(rp) =>
            Ok(inProgressPage(
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
              agentApplication = agentApplication,
              dateOfDecision = displayDateForLang(Some(projectedDecisionDate)),
              dateSubmitted = displayDateForLang(Some(
                submittedAt
                  .atZone(ZoneId.systemDefault())
                  .toLocalDate
              ))
            ))
          case failedFixable: RiskingProgress.FailedFixable =>
            Ok(failedFixableStartPage(
              actualDecisionDate = displayDateForLang(Some(failedFixable.riskingCompletedDate)),
              correctiveActionExpiryDate = displayDateForLang(failedFixable.correctiveActionExpiryDate),
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            ))
          case failedNonFixable: RiskingProgress.FailedNonFixable =>
            Ok(failedNonFixablePage(
              failedNonFixable = failedNonFixable,
              agentApplication = agentApplication,
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            ))
          case RiskingProgress.Approved => Redirect(appConfig.asaDashboardUrl) // this shouldn't really happen as the auth action should have done the redirect already

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
        h1 = "You cannot use this service",
        bodyText = Some(
          "Placeholder for the generic exit page."
        )
      ))

  private def calculateDecisionDate(submittedAt: Instant): LocalDate =
    submittedAt
      .plus(appConfig.applicationDecisionLeadTime.toMillis, ChronoUnit.MILLIS)
      .atZone(ZoneId.systemDefault())
      .toLocalDate

  /** An application status is returned as in-progress if it has been submitted for risking or if it has failed with a fixable failure and the fixable failures
    * feature is not yet enabled.
    */
  private def isInProgress(rp: RiskingProgress): Boolean =
    rp match
      case RiskingProgress.SubmittedForRisking => true
      case _: RiskingProgress.FailedFixable => !appConfig.Features.fixableFailures
      case _ => false
