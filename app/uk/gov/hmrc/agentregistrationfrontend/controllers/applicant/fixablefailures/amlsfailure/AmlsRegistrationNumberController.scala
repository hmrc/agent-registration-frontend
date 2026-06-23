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

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.AmlsRegistrationNumberPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmlsRegistrationNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AmlsRegistrationNumberPage,
  applicationService: AgentApplicationService,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .behindFeatureFlag(appConfig.Features.fixableFailures):
        implicit request =>
          val supervisoryBody = request.agentApplication.getFixableAmlsDetails.supervisoryBody
          val form: Form[AmlsRegistrationNumber] = AmlsRegistrationNumberForm(supervisoryBody)
            .form
            .fill:
              request
                .agentApplication
                .getFixableAmlsDetails
                .amlsRegistrationNumber
          Ok(view(form))

  def submit: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .ensureValidFormAndRedirectIfSaveForLater(
        form = request => AmlsRegistrationNumberForm(request.get[AgentApplication].getFixableAmlsDetails.supervisoryBody).form,
        resultToServeWhenFormHasErrors = implicit r => view(_)
      )
      .async:
        implicit request: RequestWithData[AmlsRegistrationNumber *: DataWithApplicationAndBpr] =>
          val agentApplication: AgentApplication = request.get
          val amlsRegistrationNumber: AmlsRegistrationNumber = request.get
          val updatedAmlsDetails: AmlsDetails = agentApplication.getFixableAmlsDetails
            .modify(_.amlsRegistrationNumber).setTo(Some(amlsRegistrationNumber))
          val updatedFixes: Seq[EntityFix] =
            agentApplication
              .getRiskingOutcomeEntity match
              case f: RiskingOutcomeEntity.FailedFixable =>
                f.fixes.map:
                  case a: AmlsFix => a.modify(_.amlsDetails).setTo(Some(updatedAmlsDetails))
                  case other: EntityFix => other
              case _ => throw new IllegalStateException("Risking outcome is not fixable. Cannot submit Amls registration number.")
          applicationService.upsert(
            agentApplication
              .modify(_.riskingOutcomeEntity.each)
              .using:
                case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
                case other => other
          )
            .map(_ => Redirect(AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url))
      .redirectIfSaveForLater
