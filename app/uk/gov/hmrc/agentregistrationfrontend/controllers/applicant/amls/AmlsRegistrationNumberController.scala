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

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.amls.AmlsRegistrationNumberPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmlsRegistrationNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AmlsRegistrationNumberPage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplicationInProgress
    .ensure(
      _.agentApplication.amlsDetails.isDefined,
      implicit r =>
        logger.warn("Missing AmlsDetails, redirecting to AmlsSupervisor page")
        Redirect(AppRoutes.apply.amls.AmlsSupervisorController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val supervisoryBody = request.agentApplication.getAmlsDetails.supervisoryBody
      val form: Form[AmlsRegistrationNumber] = AmlsRegistrationNumberForm(supervisoryBody)
        .form
        .fill:
          request
            .agentApplication
            .getAmlsDetails
            .amlsRegistrationNumber
      Ok(view(form))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(
        form = request => AmlsRegistrationNumberForm(request.get[AgentApplication].getAmlsDetails.supervisoryBody).form,
        resultToServeWhenFormHasErrors = implicit r => view(_)
      )
      .async:
        implicit request: RequestWithData[AmlsRegistrationNumber *: DataWithApplication] =>
          val amlsRegistrationNumber: AmlsRegistrationNumber = request.get[AmlsRegistrationNumber]
          applicationService
            .upsert(
              request.get[AgentApplication]
                .modify(_.amlsDetails.each.amlsRegistrationNumber)
                .setTo(Some(amlsRegistrationNumber))
            )
            .map: _ =>
              Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
