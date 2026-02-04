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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.TelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.TelephoneNumberPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelephoneNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: TelephoneNumberPage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .Applicant
    .getApplicationInProgress
    .ensure4(
      _.agentApplication.applicantContactDetails.map(_.applicantName).nonEmpty,
      implicit request =>
        logger.warn("Because we don't have name details we are redirecting to that page")
        Redirect(AppRoutes.apply.applicantcontactdetails.ApplicantNameController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        TelephoneNumberForm.form
          .fill:
            request
              .agentApplication
              .getApplicantContactDetails
              .telephoneNumber
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater4(TelephoneNumberForm.form, implicit r => view(_))
      .async:
        implicit request =>
          val validFormData: TelephoneNumber = request.get
          val updatedApplication: AgentApplication = request.agentApplication
            .modify(_.applicantContactDetails.each.telephoneNumber)
            .setTo(Some(validFormData))
          agentApplicationService.upsert(updatedApplication).map: _ =>
            Redirect(AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
