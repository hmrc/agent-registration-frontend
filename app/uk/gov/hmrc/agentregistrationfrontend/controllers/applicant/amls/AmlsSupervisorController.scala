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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsCodeForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.amls.AmlsSupervisoryBodyPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class AmlsSupervisorController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AmlsSupervisoryBodyPage,
  applicationService: AgentApplicationService,
  amlsCodeForm: AmlsCodeForm
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord:
      implicit request: RequestWithData[BusinessPartnerRecordResponse *: DataWithApplication] =>
        Ok(view(
          form = amlsCodeForm.form.fill:
            request
              .agentApplication
              .amlsDetails
              .map(_.supervisoryBody)
          ,
          entityName = request.get[BusinessPartnerRecordResponse].getEntityName
        ))

  def submit: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .getBusinessPartnerRecord
      .ensureValidFormAndRedirectIfSaveForLater(
        form = amlsCodeForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[AmlsSupervisoryBodyCode]) =>
              view(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName
              )
      )
      .async:
        implicit request: RequestWithData[AmlsSupervisoryBodyCode *: BusinessPartnerRecordResponse *: DataWithApplication] =>
          val supervisoryBody: AmlsSupervisoryBodyCode = request.get
          val agentApplication: AgentApplication = request.get
          if agentApplication.amlsDetails.exists(_.supervisoryBody === supervisoryBody) &&
            agentApplication.getAmlsDetails.amlsRegistrationNumber.isDefined
          then
            Future.successful(Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url)) // if same body is submitted and there is already a registration number then use CYA for navigation
          else
            applicationService
              .upsert(
                agentApplication
                  .modify(_.amlsDetails)
                  .using:
                    case Some(details: AmlsDetails) =>
                      Some(details
                        .modify(_.supervisoryBody).setTo(supervisoryBody)
                        .modify(_.amlsRegistrationNumber).setTo(None) // Clear AMLS registration number when supervisory body changes as the format is dependent on the body
                      )
                    case None =>
                      Some(AmlsDetails(
                        supervisoryBody = supervisoryBody,
                        amlsRegistrationNumber = None,
                        amlsEvidence = None
                      ))
              )
              .map(_ => Redirect(AppRoutes.apply.amls.AmlsRegistrationNumberController.show.url))
      .redirectIfSaveForLater
