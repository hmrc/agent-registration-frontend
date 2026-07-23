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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.soletraderfailures

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.model.SoleTraderFix
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.soletraderfailures.SoleTraderFailureDetailsPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class FixableSoleTraderFailureController @Inject (
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService
)(
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  soleTraderFailurePage: SoleTraderFailureDetailsPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  private type DataWithSoleTraderFix = SoleTraderFix *: RiskingOutcomeApplication.FailedFixable *: IndividualProvidedDetails *: DataWithApplicationAndBpr

  private def baseAction(
    failureCode: String
  ): ActionBuilderWithData[DataWithSoleTraderFix] = actions
    .getApplicationAfterSentForRisking
    .behindFeatureFlag(appConfig.Features.fixableFailures)
    .refine:
      implicit request =>
        individualProvideDetailsService.findAllByApplicationId(request.get[AgentApplication].agentApplicationId).map:
          case soleTrader :: Nil => request.add[IndividualProvidedDetails](soleTrader)
          case _ =>
            logger.warn(s"Unexpected variation on sole trader individuals for failure code $failureCode, redirecting to where outcome can be handled.")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
    .refine:
      implicit request: RequestWithData[IndividualProvidedDetails *: DataWithApplicationAndBpr] =>
        request.get[AgentApplication].riskingOutcomeApplication match
          case Some(outcome: RiskingOutcomeApplication.FailedFixable) => request.add[RiskingOutcomeApplication.FailedFixable](outcome)
          case outcome =>
            logger.warn(s"Risking outcome for application is not fixable (or missing). Redirecting to where outcome can be handled: $outcome")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
    .refine:
      implicit request: RequestWithData[RiskingOutcomeApplication.FailedFixable *: IndividualProvidedDetails *: DataWithApplicationAndBpr] =>
        val failureReasonCode: String = extractReasonCode(failureCode)
        val individualFix: Option[IndividualFix] =
          request.get[IndividualProvidedDetails].riskingOutcomeIndividual match
            case Some(outcome: RiskingOutcomeIndividual.FailedFixable) => outcome.fixes.find(fix => extractReasonCode(fix.toString) === failureReasonCode)
            case _ => None
        val entityFix: Option[EntityFix] =
          request.get[AgentApplication].getRiskingOutcomeEntity match
            case outcome: RiskingOutcomeEntity.FailedFixable => outcome.fixes.find(fix => extractReasonCode(fix.toString) === extractReasonCode(failureCode))
            case _ => None
        request.add[SoleTraderFix](SoleTraderFix(entityFix, individualFix))

  def show(failureCode: String): Action[AnyContent] =
    baseAction(failureCode):
      implicit request =>
        val outcome: RiskingOutcomeApplication.FailedFixable = request.get
        val soleTraderFix: SoleTraderFix = request.get
        Ok(soleTraderFailurePage(
          failureCode = failureCode,
          correctiveActionExpiryDate = displayDateForLang(outcome.correctiveActionExpiryDate),
          form = ConfirmFixForm.form(failureCode).fill:
            soleTraderFix.isConfirmed
        ))

  def submit(failureCode: String): Action[AnyContent] = baseAction(failureCode)
    .ensureValidForm(
      form = implicit request => ConfirmFixForm.form(failureCode),
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[Boolean]) =>
            soleTraderFailurePage(
              failureCode = failureCode,
              correctiveActionExpiryDate = displayDateForLang(request.get[RiskingOutcomeApplication.FailedFixable].correctiveActionExpiryDate),
              form = formWithErrors
            )
    )
    .async:
      implicit request =>
        request.get[SoleTraderFix] match
          case SoleTraderFix(Some(entityFix), None) =>
            updateEntityFix(entityFix, request).map: _ =>
              Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
          case SoleTraderFix(None, Some(individualFix)) =>
            updateIndividualFix(individualFix, request).map: _ =>
              Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
          case SoleTraderFix(Some(entityFix), Some(individualFix)) =>
            // Update both the entity fix and the individual fix
            for
              _ <- updateEntityFix(entityFix, request)
              _ <- updateIndividualFix(individualFix, request)
            yield Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
          case SoleTraderFix(None, None) =>
            logger.warn(s"No matching entity or individual fix found for failure code $failureCode. Redirecting to task list.")
            Future.successful(Redirect(AppRoutes.fixablefailures.FixableTaskListController.show))

  private def extractReasonCode(failureCode: String): String = failureCode.split("\\.").drop(1).mkString(".")

  private def updateEntityFix(
    entityFix: EntityFix,
    request: RequestWithData[Boolean *: DataWithSoleTraderFix]
  )(using RequestHeader): Future[Unit] =
    // Update the matching entity fix in the riskingOutComeEntity
    val updatedFixes: Seq[EntityFix] =
      request.agentApplication
        .getRiskingOutcomeEntity match
        case entityOutcome: RiskingOutcomeEntity.FailedFixable =>
          entityOutcome.fixes.map:
            case f: EntityFix if f === entityFix => f.modify(_.isConfirmed).setTo(Some(request.get[Boolean]))
            case other => other
        case _ => throw new IllegalStateException("Risking outcome for entity is not fixable. Cannot update fixes.")
    agentApplicationService.upsert(
      request.agentApplication
        .modify(_.riskingOutcomeEntity.each)
        .using:
          case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
          case other => other
    )

  private def updateIndividualFix(
    individualFix: IndividualFix,
    request: RequestWithData[Boolean *: DataWithSoleTraderFix]
  )(using RequestHeader): Future[Unit] =
    // Update the matching individual fix in the riskingOutcomeIndividual
    val individualProvidedDetails: IndividualProvidedDetails = request.get
    individualProvidedDetails.riskingOutcomeIndividual match
      case Some(outcome: RiskingOutcomeIndividual.FailedFixable) =>
        val updatedFixes: Seq[IndividualFix] = outcome.fixes.map:
          case f: IndividualFix if f === individualFix => f.modify(_.isConfirmed).setTo(Some(request.get[Boolean]))
          case other => other
        individualProvideDetailsService.upsertForApplication(
          individualProvidedDetails
            .modify(_.riskingOutcomeIndividual.each)
            .using:
              case f: RiskingOutcomeIndividual.FailedFixable => f.copy(fixes = updatedFixes)
              case other => other
        )
      case _ => throw new IllegalStateException("Risking outcome for individual is not fixable. Cannot update fixes.")
