/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated

import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsUnincorporatedPartnership
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnterKeyIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterIndividualNamePageRenderer: EnterIndividualNamePageRenderer,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  /** A `def`, not a `val`: reverse routes read the router prefix, which Play only sets after the controllers are built, so a `val` here would drop the
    * `/agent-registration` prefix.
    */
  private def formAction: Call = AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.submit

  def show: Action[AnyContent] = actions
    .getKeyIndividualsForUnincorporatedPartnership
    .async:
      implicit request =>
        enterIndividualNamePageRenderer
          .render(
            form = IndividualNameForm.form,
            formAction = formAction
          )
          .map(Ok(_))

  def submit: Action[AnyContent] =
    actions
      .getKeyIndividualsForUnincorporatedPartnership
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = IndividualNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              enterIndividualNamePageRenderer.render(
                form = formWithErrors,
                formAction = formAction
              )
      )
      .async:
        implicit request =>
          val individualName: IndividualName = request.get
          for
            individualProvidedDetails: IndividualProvidedDetails <- individualProvideDetailsService.create(
              individualName = individualName,
              isPersonOfControl = true,
              agentApplicationId = request.get[IsUnincorporatedPartnership].agentApplicationId
            )
            _ <- individualProvideDetailsService.upsertForApplication(individualProvidedDetails)
          yield Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show)
      .redirectIfSaveForLater
