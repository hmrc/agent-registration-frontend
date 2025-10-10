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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantRoleInLlpForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.ApplicantRoleInLlpPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.chaining.scalaUtilChainingOps

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
      val form: Form[ApplicantRoleInLlp] = ApplicantRoleInLlpForm.form.fill:
        request
          .agentApplication
          .applicantContactDetails.map(_.applicantName.role)
      Ok(view(form))

  def submit: Action[AnyContent] =
    actions
      .getApplicationInProgress
      .ensureValidFormAndRedirectIfSaveForLater(ApplicantRoleInLlpForm.form, implicit r => view(_))
      .async:
        // Hint: Explicit type annotation helps IDE provide better code completion
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[ApplicantRoleInLlp]) =>
          val applicantRoleFromForm: ApplicantRoleInLlp = request.formValue

          val updatedApplication: AgentApplication = request
            .agentApplication
            .modify(_.applicantContactDetails).using:
              case None => // applicant selects role for the first time
                applicantRoleFromForm match
                  case ApplicantRoleInLlp.Member => ApplicantName.NameOfMember().pipe(ApplicantContactDetails.apply).pipe(Some(_))
                  case ApplicantRoleInLlp.Authorised => ApplicantName.NameOfAuthorised().pipe(ApplicantContactDetails.apply).pipe(Some(_))
              case Some(applicantContactDetails) => // applicant selects updates selected role (probably came back from previous pages)
                applicantContactDetails.modify(_.applicantName).using:
                  case n: ApplicantName.NameOfMember =>
                    applicantRoleFromForm match
                      case ApplicantRoleInLlp.Member => n // don't change anything, same selection as previously
                      case ApplicantRoleInLlp.Authorised => ApplicantName.NameOfAuthorised()
                  case n: ApplicantName.NameOfAuthorised =>
                    applicantRoleFromForm match
                      case ApplicantRoleInLlp.Member => ApplicantName.NameOfMember()
                      case ApplicantRoleInLlp.Authorised => n // don't change anything, same selection as previously
                .pipe(Some(_))

          applicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(
                applicantRoleFromForm match
                  case ApplicantRoleInLlp.Member => routes.MemberNameController.show.url
                  case ApplicantRoleInLlp.Authorised => routes.AuthorisedNameController.show.url
              )
      .redirectIfSaveForLater
