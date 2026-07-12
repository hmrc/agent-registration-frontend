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
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskedEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.ConfirmationPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.ConfirmationPage as ResubmissionConfirmationPage
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
import scala.concurrent.Future

@Singleton
class AgentApplicationController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  simplePage: SimplePage,
  confirmationPage: ConfirmationPage,
  resubmissionConfirmationPage: ResubmissionConfirmationPage,
  inProgressPage: InProgressPage,
  failedNonFixablePage: FailedNonFixablePage,
  failedFixableStartPage: FailedFixableStartPage,
  viewApplicationPage: ViewApplicationPage,
  appConfig: AppConfig,
  agentRegistrationRiskingService: AgentRegistrationRiskingService,
  individualProvideDetailsService: IndividualProvideDetailsService
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
    .getApplicationAfterSentForRisking
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      individualProvideDetailsService.findAllByApplicationId(agentApplication.agentApplicationId).map: individualsList =>
        request.add[List[IndividualProvidedDetails]](individualsList)
    )
    .async:
      implicit request =>
        if appConfig.Features.fixableFailures
        then Future.successful(useApplicationForStatus(request))
        else useRiskingServiceForStatus(request)

  def viewSubmittedApplication: Action[AnyContent] = actions
    .getApplicationAfterSentForRisking:
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

  /** We only need this method for as long as the feature flag for Fixable Failures is turned off. So we've moved the call to the risking service out of the
    * action refiners and into this method instead
    */
  private def useRiskingServiceForStatus(request: RequestWithData[List[IndividualProvidedDetails] *: DataWithApplicationAndBpr])(using
    RequestHeader
  ): Future[Result] =
    val agentApplication: AgentApplication = request.get
    val submittedAt: Instant = agentApplication.getSubmittedAt
    val projectedDecisionDate: LocalDate = calculateDecisionDate(submittedAt)
    agentRegistrationRiskingService
      .getRiskingProgress(request.agentApplication.applicationReference)
      .map:
        case RiskingProgress.ReadyForSubmission => // show the confirmation screen
          Ok(confirmationPage(
            dateOfDecision = displayDateForLang(Some(projectedDecisionDate)),
            agentApplication = agentApplication
          ))
        case RiskingProgress.SubmittedForRisking | _: RiskingProgress.FailedFixable =>
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
        case failedNonFixable: RiskingProgress.FailedNonFixable =>
          Ok(failedNonFixablePage(
            failedNonFixable = failedNonFixable,
            agentApplication = agentApplication,
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName
          ))
        case RiskingProgress.Approved => Redirect(appConfig.asaDashboardUrl) // this shouldn't really happen as the auth action should have done the redirect already

  /** This is the method we want to use once the feature flag for Fixable Failures is turned on.
    */
  private def useApplicationForStatus(request: RequestWithData[List[IndividualProvidedDetails] *: DataWithApplicationAndBpr])(using
    RequestHeader
  ): Result =
    val agentApplication: AgentApplication = request.get
    val submittedAt: Instant = agentApplication.getSubmittedAt
    val reSubmittedAt: Option[Instant] =
      agentApplication.riskingOutcomeApplication match
        case Some(riskingOutcomeApplication: RiskingOutcomeApplication.FailedFixable) => riskingOutcomeApplication.reSubmittedAt
        case _ => None
    val projectedDecisionDate: LocalDate = calculateDecisionDate(submittedAt)
    agentApplication.applicationState match
      case ApplicationState.Started | ApplicationState.GrsDataReceived =>
        throw new IllegalStateException(
          s"Application is in state ${agentApplication.applicationState} but the application status endpoint should only be called after the application has been submitted for risking"
        )
      case ApplicationState.SentForRisking =>
        reSubmittedAt.fold(
          Ok(confirmationPage(
            dateOfDecision = displayDateForLang(Some(projectedDecisionDate)),
            agentApplication = agentApplication
          ))
        )(d =>
          Ok(resubmissionConfirmationPage(
            dateOfDecision = displayDateForLang(Some(calculateDecisionDate(d))),
            agentApplication = agentApplication
          ))
        )
      case ApplicationState.SentToMinerva =>
        reSubmittedAt.fold(
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
        )(d =>
          Ok(resubmissionConfirmationPage(
            dateOfDecision = displayDateForLang(Some(calculateDecisionDate(d))),
            agentApplication = agentApplication
          ))
        )
      case ApplicationState.RiskingCompleted =>
        val riskingOutcomeApplication: RiskingOutcomeApplication = agentApplication.riskingOutcomeApplication.getOrThrowExpectedDataMissing(
          s"Risking completed but no outcome found for application ${agentApplication.applicationReference}"
        )
        riskingOutcomeApplication match
          case riskingOutcomeApplication: RiskingOutcomeApplication.FailedFixable =>
            Ok(failedFixableStartPage(
              actualDecisionDate = displayDateForLang(Some(riskingOutcomeApplication.actualDecisionDate)),
              correctiveActionExpiryDate = displayDateForLang(riskingOutcomeApplication.correctiveActionExpiryDate),
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            ))
          case riskingOutcomeApplication: RiskingOutcomeApplication.FailedNonFixable =>
            val riskedEntity: RiskingOutcomeEntity = agentApplication.riskingOutcomeEntity.getOrThrowExpectedDataMissing(
              s"Risking completed but no outcome found for entity ${agentApplication.applicationReference}"
            )
            val riskedIndividuals: List[IndividualProvidedDetails] = request.get
            Ok(failedNonFixablePage(
              failedNonFixable = RiskingProgress.FailedNonFixable(
                riskedEntity = RiskedEntity(
                  applicationReference = agentApplication.applicationReference,
                  failures =
                    riskedEntity match
                      case f: RiskingOutcomeEntity.FailedNonFixable => f.failures
                      case _ => Seq.empty
                ),
                riskedIndividuals = riskedIndividuals.map: individual =>
                  RiskedIndividual(
                    personReference = individual.personReference,
                    individualName = individual.individualName,
                    failures =
                      individual.riskingOutcomeIndividual match
                        case Some(riskedIndividual: RiskingOutcomeIndividual.FailedNonFixable) => riskedIndividual.failures
                        case _ => Seq.empty
                  ),
                riskingCompletedDate = riskingOutcomeApplication.actualDecisionDate,
                correctiveActionExpiryDate = Some(riskingOutcomeApplication.correctiveActionExpiryDate)
              ),
              agentApplication = agentApplication,
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            ))
          case riskingOutcomeApplication: RiskingOutcomeApplication.Approved =>
            // Fallback case: Application outcome is Approved, but account setup is incomplete.
            // Not sure if this can ever happen.
            // This might occur when risking approval succeeded and BE was notified, but the ASA account provisioning
            // in agent-registration-risking failed or is still in progress.
            // Redirect to ASA anyway as in the original implementation
            logger.warn(s"Application ${agentApplication.applicationReference} approved but ASA account not fully provisioned. Redirecting to ASA dashboard")
            Redirect(appConfig.asaDashboardUrl)
