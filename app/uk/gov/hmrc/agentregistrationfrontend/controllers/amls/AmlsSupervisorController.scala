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
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectFromOptionsForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper.getSubmitAction
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsSupervisoryBodyPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AmlsSupervisorController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: AmlsSupervisoryBodyPage,
  applicationService: ApplicationService,
  amlsCodes: AmlsCodes
)(implicit ec: ExecutionContext)
extends FrontendController(mcc)
with I18nSupport:

  def show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      val formWithOptions = SelectFromOptionsForm.form("amlsSupervisoryBody", amlsCodes.amlsCodes.keys.toSeq)
      val form: Form[String] =
        request
          .agentApplication
          .amlsDetails
          .fold(formWithOptions)((amlsDetails: AmlsDetails) =>
            formWithOptions.fill(amlsDetails.supervisoryBody)
          )
      Ok(view(form))

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      SelectFromOptionsForm.form("amlsSupervisoryBody", amlsCodes.amlsCodes.keys.toSeq)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              if getSubmitAction(request).isSaveAndComeBackLater
              then
                Redirect(applicationRoutes.AgentApplicationController.saveAndComeBackLater.url)
              else
                BadRequest(view(formWithErrors))
            ),
          supervisoryBody =>
            applicationService
              .upsert(
                request.agentApplication
                  .modify(_.amlsDetails)
                  .using {
                    case Some(details) =>
                      Some(details
                        .modify(_.supervisoryBody)
                        .setTo(supervisoryBody))
                    case None =>
                      Some(AmlsDetails(
                        supervisoryBody = supervisoryBody
                      ))
                  }
              )
              .map(_ =>
                Redirect(
                  if getSubmitAction(request)
                      .isSaveAndComeBackLater
                  then applicationRoutes.AgentApplicationController.saveAndComeBackLater.url
                  else routes.AmlsRegistrationNumberController.show.url
                )
              )
        )
