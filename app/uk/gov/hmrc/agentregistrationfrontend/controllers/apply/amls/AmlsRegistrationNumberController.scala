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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsRegistrationNumberPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmlsRegistrationNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AmlsRegistrationNumberPage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.amlsDetails.isDefined,
      implicit r =>
        logger.warn("Missing AmlsDetails, redirecting to AmlsSupervisor page")
        Redirect(AppRoutes.apply.amls.AmlsSupervisorController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val isHmrc = request.agentApplication.getAmlsDetails.isHmrc
      val form: Form[AmlsRegistrationNumber] = AmlsRegistrationNumberForm(isHmrc).form.fill(request
        .agentApplication
        .getAmlsDetails
        .amlsRegistrationNumber)
      Ok(view(form))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(
        r => AmlsRegistrationNumberForm(r.agentApplication.getAmlsDetails.isHmrc).form,
        implicit r => view(_)
      )
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[AmlsRegistrationNumber]) =>
          val amlsRegistrationNumber: AmlsRegistrationNumber = request.formValue

          applicationService
            .deleteMeUpsert(
              request.agentApplication
                .modify(_.amlsDetails.each.amlsRegistrationNumber)
                .setTo(Some(amlsRegistrationNumber))
            )
            .map(_ =>
              Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url)
            )
      .redirectIfSaveForLater
