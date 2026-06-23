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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import com.google.inject.Inject
import com.google.inject.Singleton
import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject (
  agentApplicationService: AgentApplicationService
)(
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .behindFeatureFlag(appConfig.Features.fixableFailures)
      .ensure(
        r => r.agentApplication.getFixableAmlsDetails.isComplete,
        implicit request =>
          logger.debug(s"Cannot display Check Your Answers page - incomplete AMLS details.")
          request.agentApplication.getFixableAmlsDetails match
            case AmlsDetails(
                  AmlsSupervisoryBodyCode(_),
                  None,
                  _
                ) =>
              Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show)
            case AmlsDetails(
                  AmlsSupervisoryBodyCode(amlsCode),
                  Some(_),
                  _
                ) if !amlsCode.contains("HMRC") =>
              Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.show)
            case _ => Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show)
      ):
        implicit request => Ok(view(request.agentApplication.getFixableAmlsDetails))

  def submit: Action[AnyContent] = actions
    .getApplicationAfterSentForRisking
    .ensure(
      r => r.agentApplication.getFixableAmlsDetails.isComplete,
      implicit request =>
        logger.debug(s"Cannot submit Check Your Answers page - incomplete AMLS details.")
        request.agentApplication.getFixableAmlsDetails match
          case AmlsDetails(
                AmlsSupervisoryBodyCode(_),
                None,
                _
              ) =>
            Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show)
          case AmlsDetails(
                AmlsSupervisoryBodyCode(amlsCode),
                Some(_),
                _
              ) if !amlsCode.contains("HMRC") =>
            Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.show)
          case _ => Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show)
    )
    .async:
      implicit request =>
        val updatedFixes: Seq[EntityFix] =
          request.agentApplication
            .getRiskingOutcomeEntity match
            case f: RiskingOutcomeEntity.FailedFixable =>
              f.fixes.map:
                case a: AmlsFix => a.modify(_.isConfirmed).setTo(Some(true))
                case other => other
            case _ => throw new IllegalStateException("Risking outcome is not fixable. Cannot submit Check Your Answers page.")
        agentApplicationService.upsert(
          request.agentApplication
            .modify(_.riskingOutcomeEntity.each)
            .using:
              case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
              case other => other
        ).map: _ =>
          Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
