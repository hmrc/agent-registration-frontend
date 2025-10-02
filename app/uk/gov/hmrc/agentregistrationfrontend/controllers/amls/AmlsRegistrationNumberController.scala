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

package uk.gov.hmrc.agentregistrationfrontend.controllers.amls

import com.softwaremill.quicklens.*
import play.api.data.Form

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper.getSubmitAction
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsRegistrationNumberPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class AmlsRegistrationNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AmlsRegistrationNumberPage,
  applicationService: ApplicationService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      val isHmrc = request.agentApplication.getAmlsDetails.isHmrc
      val emptyForm = AmlsRegistrationNumberForm(isHmrc).form
      val form: Form[AmlsRegistrationNumber] =
        request
          .agentApplication
          .getAmlsDetails
          .amlsRegistrationNumber
          .fold(emptyForm)((amlsRegistrationNumber: AmlsRegistrationNumber) =>
            emptyForm.fill(amlsRegistrationNumber)
          )
      Ok(view(form))

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      val isHmrc = request.agentApplication.getAmlsDetails.isHmrc
      AmlsRegistrationNumberForm(isHmrc).form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              if getSubmitAction(request)
                  .isSaveAndComeBackLater
              then Redirect(applicationRoutes.SaveForLaterController.show.url)
              else BadRequest(view(formWithErrors))
            ),
          amlsRegistrationNumber =>
            applicationService
              .upsert(
                request.agentApplication
                  .modify(_.amlsDetails.each.amlsRegistrationNumber)
                  .setTo(Some(amlsRegistrationNumber))
              )
              .map(_ =>
                Redirect(
                  if getSubmitAction(request)
                      .isSaveAndComeBackLater
                  then applicationRoutes.SaveForLaterController.show.url
                  else if isHmrc then routes.CheckYourAnswersController.show.url
                  else routes.AmlsExpiryDateController.show.url
                )
              )
        )
