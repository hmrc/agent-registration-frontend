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
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AuthorisedNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.AuthorisedNamePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorisedNameController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AuthorisedNamePage,
  applicationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication.asLlpApplication.applicantContactDetails.map(_.applicantName.role).contains(ApplicantRoleInLlp.Authorised),
      implicit request =>
        logger.warn("Authorised name page requires Authorised role. Redirecting to applicant role selection page")
        Redirect(routes.ApplicantRoleInLlpController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        AuthorisedNameForm.form
          .fill:
            request.agentApplication.asLlpApplication.authorisedName
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(AuthorisedNameForm.form, implicit r => view(_))
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[String]) =>
          val validFormData: String = request.formValue
          val updatedApplication: AgentApplication = request.agentApplication.asLlpApplication
            .modify(_.applicantContactDetails.each.applicantName)
            .setTo(ApplicantName.NameOfAuthorised(
              name = Some(validFormData)
            ))
          applicationService.upsert(updatedApplication).map: _ =>
            Redirect(routes.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  extension (agentApplication: AgentApplicationLlp)
    private def authorisedName: Option[String] =
      for
        acd <- agentApplication.applicantContactDetails
        authorisedName <- acd.applicantName.as[ApplicantName.NameOfAuthorised]
        name <- authorisedName.name
      yield name
