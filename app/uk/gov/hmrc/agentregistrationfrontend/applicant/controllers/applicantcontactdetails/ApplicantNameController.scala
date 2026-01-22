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

package uk.gov.hmrc.agentregistrationfrontend.applicant.controllers.applicantcontactdetails

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.shared.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.ApplicantNamePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicantNameController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: ApplicantNamePage,
  applicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress:
      implicit request =>
        Ok(view(
          ApplicantNameForm.form
            .fill:
              request
                .agentApplication
                .applicantContactDetails
                .map(_.applicantName)
        ))

  def submit: Action[AnyContent] =
    actions
      .Applicant
      .getApplicationInProgress
      .ensureValidFormAndRedirectIfSaveForLater(ApplicantNameForm.form, implicit r => view(_))
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[ApplicantName]) =>
          val validFormData: ApplicantName = request.formValue
          val updatedApplication: AgentApplication = request.agentApplication
            .modify(_.applicantContactDetails)
            .using:
              case None => // applicant enters
                Some(
                  ApplicantContactDetails(
                    applicantName = validFormData
                  )
                )
              case Some(details) => // applicant updates
                Some(
                  details
                    .modify(_.applicantName)
                    .setTo(validFormData)
                )

          applicationService.upsert(updatedApplication).map: _ =>
            Redirect(AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater
