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

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.TelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.TelephoneNumberPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelephoneNumberController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: TelephoneNumberPage,
  agentRegistrationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication.asLlpApplication.applicantContactDetails.map(_.applicantName) match {
        case Some(ApplicantName.NameOfMember(_, Some(_))) => true
        case Some(ApplicantName.NameOfAuthorised(Some(_))) => true
        case _ => false
      },
      implicit request =>
        logger.warn("Because we don't have name details we are computing which name type to redirect to and redirecting to that page")
        request.agentApplication.asLlpApplication.applicantContactDetails.map(_.applicantName) match {
          case Some(ApplicantName.NameOfMember(_, _)) => Redirect(routes.CompaniesHouseMatchingController.show)
          case Some(ApplicantName.NameOfAuthorised(_)) => Redirect(routes.AuthorisedNameController.show)
          case _ => Redirect(routes.ApplicantRoleInLlpController.show)
        }
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        TelephoneNumberForm.form
          .fill:
            request
              .agentApplication
              .asLlpApplication
              .getApplicantContactDetails
              .telephoneNumber
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(TelephoneNumberForm.form, implicit r => view(_))
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[TelephoneNumber]) =>
          val validFormData: TelephoneNumber = request.formValue
          val updatedApplication: AgentApplication = request.agentApplication.asLlpApplication
            .modify(_.applicantContactDetails.each.telephoneNumber)
            .setTo(Some(validFormData))
          agentRegistrationService.upsert(updatedApplication).map: _ =>
            Redirect(routes.EmailAddressController.show.url)
      .redirectIfSaveForLater
