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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.DisplayDate.displayDateForLang
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.FixableIndividualFailurePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableIndividualFailureController @Inject (individualProvidedDetailsService: IndividualProvideDetailsService)(
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  view: FixableIndividualFailurePage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def baseAction(
    fixCode: String,
    linkId: LinkId
  ): ActionBuilderWithData[IndividualFix *: DataWithFailedFixable] = actions
    .authorisedWithFailedFixable(linkId)
    .behindFeatureFlag(appConfig.Features.fixableFailures)
    .refine(implicit request =>
      request.get[RiskingOutcomeIndividual.FailedFixable].fixes.find(_.toString === fixCode) match
        case Some(individualFix) => request.add[IndividualFix](individualFix)
        case None =>
          logger.warn(s"Risking outcome for individual does not contain a fix with code $fixCode.")
          NotFound
    )

  def show(
    fixCode: String,
    linkId: LinkId
  ): Action[AnyContent] =
    baseAction(fixCode, linkId):
      implicit request =>
        val overallOutcome: RiskingOutcomeApplication = request.get
        Ok(view(
          entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
          failureCode = fixCode,
          correctiveActionExpiryDate = displayDateForLang(overallOutcome.correctiveActionExpiryDate),
          form = ConfirmFixForm.form(fixCode).fill:
            request.get[IndividualFix].isConfirmed
          ,
          linkId = linkId
        ))

  def submit(
    fixCode: String,
    linkId: LinkId
  ): Action[AnyContent] = baseAction(fixCode, linkId)
    .ensureValidForm(
      form = implicit request => ConfirmFixForm.form(fixCode),
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[Boolean]) =>
            view(
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
              failureCode = fixCode,
              correctiveActionExpiryDate = displayDateForLang(request.get[RiskingOutcomeApplication].correctiveActionExpiryDate),
              form = formWithErrors,
              linkId = linkId
            )
    )
    .async:
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val updatedFixes: Seq[IndividualFix] = request.get[RiskingOutcomeIndividual.FailedFixable].fixes.map:
          case f: IndividualFix if f === request.get[IndividualFix] => f.modify(_.isConfirmed).setTo(Some(request.get[Boolean]))
          case other => other
        individualProvidedDetailsService.upsert(
          individualProvidedDetails
            .modify(_.riskingOutcomeIndividual.each)
            .using:
              case f: RiskingOutcomeIndividual.FailedFixable => f.copy(fixes = updatedFixes)
              case other => other
        ).map: _ =>
          Redirect(AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableTaskListController.show(linkId))
