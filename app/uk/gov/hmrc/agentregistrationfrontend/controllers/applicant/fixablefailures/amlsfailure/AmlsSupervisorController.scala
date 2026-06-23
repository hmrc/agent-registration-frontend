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

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.AmlsSupervisoryBodyPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class AmlsSupervisorController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AmlsSupervisoryBodyPage,
  applicationService: AgentApplicationService,
  amlsCodeForm: AmlsCodeForm,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .behindFeatureFlag(appConfig.Features.fixableFailures):
        implicit request =>
          val form: Form[AmlsSupervisoryBodyCode] = amlsCodeForm.form.fill:
            request
              .agentApplication
              .getFixableAmlsDetails
              .supervisoryBody
          Ok(view(
            form = form,
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName
          ))

  def submit: Action[AnyContent] =
    actions
      .getApplicationAfterSentForRisking
      .ensureValidFormAndRedirectIfSaveForLater(
        amlsCodeForm.form,
        implicit request =>
          (formWithErrors: Form[AmlsSupervisoryBodyCode]) =>
            view(
              form = formWithErrors,
              entityName = request.get[BusinessPartnerRecordResponse].getEntityName
            )
      )
      .async:
        implicit request: RequestWithData[AmlsSupervisoryBodyCode *: DataWithApplicationAndBpr] =>
          val supervisoryBody: AmlsSupervisoryBodyCode = request.get
          val agentApplication: AgentApplication = request.get
          val fixableAmlsDetails: AmlsDetails = agentApplication.getFixableAmlsDetails
          if fixableAmlsDetails.supervisoryBody === supervisoryBody &&
            fixableAmlsDetails.amlsRegistrationNumber.isDefined
          then
            Future.successful(Redirect(AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url)) // if same body is submitted and there is already a registration number then use CYA for navigation
          else
            val updatedAmlsDetails: AmlsDetails = fixableAmlsDetails
              .modify(_.supervisoryBody).setTo(supervisoryBody)
              .modify(_.amlsRegistrationNumber).setTo(None)
            val updatedFixes: Seq[EntityFix] =
              agentApplication
                .getRiskingOutcomeEntity match
                case f: RiskingOutcomeEntity.FailedFixable =>
                  f.fixes.map:
                    case a: AmlsFix => a.modify(_.amlsDetails).setTo(Some(updatedAmlsDetails))
                    case other: EntityFix => other
                case _ => throw new IllegalStateException("Risking outcome is not fixable. Cannot submit supervisory body.")
            applicationService.upsert(
              agentApplication
                .modify(_.riskingOutcomeEntity.each)
                .using:
                  case f: RiskingOutcomeEntity.FailedFixable => f.copy(fixes = updatedFixes)
                  case other => other
            )
              .map(_ => Redirect(AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show.url))
      .redirectIfFixableSaveForLater
