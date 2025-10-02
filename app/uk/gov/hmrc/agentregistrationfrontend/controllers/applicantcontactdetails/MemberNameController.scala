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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper.getSubmitAction
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MemberNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MemberNameController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: MemberNamePage,
  agentRegistrationService: AgentRegistrationService
)
extends FrontendController(mcc, actions):

  val show: Action[AnyContent] = actions.getApplicationInProgress:
    implicit request =>
      Ok(view(CompaniesHouseNameQueryForm.form))

  def submit: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      CompaniesHouseNameQueryForm.form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(
            if getSubmitAction(request)
                .isSaveAndComeBackLater
            then Redirect(applicationRoutes.SaveForLaterController.show.url)
            else BadRequest(view(formWithErrors))
          ),
        validFormData =>
          agentRegistrationService.searchCompaniesHouseOfficers(validFormData)
            .map { searchResults =>
              if searchResults.isEmpty then
                Redirect("no results found page")
              else
                // TODO: store results somewhere and redirect to results page
                Redirect("results page")
            }
      )
