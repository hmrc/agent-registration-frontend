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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.entityfailures

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.entityfailures.EntityFailureDetailsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableEntityFailuresController @Inject (agentApplicationService: AgentApplicationService)(
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: EntityFailureDetailsPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  private def baseAction(failureCode: String): ActionBuilderWithData[EntityFix *: RiskingOutcomeApplication.FailedFixable *: DataWithApplicationAndBpr] =
    actions
      .getApplicationAfterSentForRisking
      .behindFeatureFlag(appConfig.Features.fixableFailures)
      .refine:
        implicit request =>
          request.get[AgentApplication].riskingOutcomeApplication match
            case Some(outcome: RiskingOutcomeApplication.FailedFixable) => request.add[RiskingOutcomeApplication.FailedFixable](outcome)
            case outcome =>
              logger.warn(s"Risking outcome for application is not fixable (or missing). Redirecting to where outcome can be handled: $outcome")
              Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
      .refine:
        implicit request =>
          request.agentApplication.getRiskingOutcomeEntity match
            case RiskingOutcomeEntity.FailedFixable(fixes: Seq[EntityFix]) =>
              val fix: Option[EntityFix] = fixes.find((fix: EntityFix) => fix.toString === failureCode)
              fix match
                case Some(fix) => request.add[EntityFix](fix)
                case None =>
                  logger.warn(s"The failure code in the url $failureCode cannot be found in the entity fixes for this application.")
                  NotFound
            case _ =>
              logger.warn("Risking outcome for entity is not fixable. Redirecting to where outcome can be handled.")
              Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)

  def show(failureCode: String): Action[AnyContent] =
    baseAction(failureCode):
      implicit request =>
        val outcome: RiskingOutcomeApplication.FailedFixable = request.get
        Ok(view(
          entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
          failureCode = failureCode,
          correctiveActionExpiryDate = displayDateForLang(outcome.correctiveActionExpiryDate),
          form = ConfirmFixForm.form(failureCode).fill:
            request.get[EntityFix].isConfirmed
        ))

  def submit(failureCode: String): Action[AnyContent] = baseAction(failureCode)
    .ensureValidForm(
      form = implicit request => ConfirmFixForm.form(failureCode),
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[Boolean]) =>
            view(
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
              failureCode = failureCode,
              correctiveActionExpiryDate = displayDateForLang(request.get[RiskingOutcomeApplication.FailedFixable].correctiveActionExpiryDate),
              form = formWithErrors
            )
    )
    .async:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        val updatedFixes: Seq[EntityFix] =
          agentApplication
            .getRiskingOutcomeEntity match
            case entityOutcome: RiskingOutcomeEntity.FailedFixable =>
              entityOutcome.fixes.map:
                case f: EntityFix if f === request.get[EntityFix] => f.modify(_.isConfirmed).setTo(Some(request.get[Boolean]))
                case other => other
            case _ => throw new IllegalStateException("Risking outcome for entity is not fixable. Cannot update fixes.")
        agentApplicationService.upsert(
          agentApplication
            .modify(_.riskingOutcomeEntity.each)
            .using:
              case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
              case other => other
        ).map: _ =>
          Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
