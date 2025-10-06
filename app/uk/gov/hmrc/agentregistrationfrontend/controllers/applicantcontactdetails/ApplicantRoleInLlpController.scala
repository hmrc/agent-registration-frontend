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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AppicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.ApplicantContactDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantRoleInLlpForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.ApplicantRoleInLlpPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class ApplicantRoleInLlpController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: ApplicantRoleInLlpPage,
  applicationService: ApplicationService
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      val form: Form[AppicantRoleInLlp] = ApplicantRoleInLlpForm.form.fill:
        request
          .agentApplication
          .applicantContactDetails.map(_.applicantRoleInLlp)
      Ok(view(form))

  def submit: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .ensureValidFormAndHandleSaveForLater(ApplicantRoleInLlpForm.form, implicit r => view(_))
      .async:
        implicit request =>
          val applicantRoleInLlp: AppicantRoleInLlp = request.formValue
          applicationService
            .upsert(
              request.agentApplication
                .modify(_.applicantContactDetails)
                .using {
                  case Some(acd) =>
                    Some(acd
                      .modify(_.applicantRoleInLlp)
                      .setTo(applicantRoleInLlp))
                  case None =>
                    Some(ApplicantContactDetails(
                      applicantRoleInLlp = applicantRoleInLlp
                    ))
                }
            )
            .map(_ =>
              Redirect(
                applicantRoleInLlp match
                  case AppicantRoleInLlp.Member => routes.MemberNameController.show.url
                  case AppicantRoleInLlp.Authorised => routes.ApplicantNameController.show.url
              )
            )
      .handleSaveAndComeBackLater
