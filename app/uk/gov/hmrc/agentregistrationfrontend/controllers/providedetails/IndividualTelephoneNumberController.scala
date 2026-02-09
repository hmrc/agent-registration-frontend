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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualTelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualTelephoneNumberPage
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions

import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualTelephoneNumberController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualTelephoneNumberPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions
    .getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.companiesHouseMatch.nonEmpty, // TODO: Add check for companies house details
      implicit request =>
        Redirect(AppRoutes.providedetails.CompaniesHouseNameQueryController.show)
    )

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[TelephoneNumber](
      IndividualTelephoneNumberForm.form,
      implicit r => view(_)
    )
    .async:
      implicit request =>
        val telephoneNumberFromForm: TelephoneNumber = request.get
        val updatedProvidedDetails: IndividualProvidedDetailsToBeDeleted = request
          .individualProvidedDetails
          .modify(_.telephoneNumber)
          .setTo(Some(telephoneNumberFromForm))
        individualProvideDetailsService
          .upsert(updatedProvidedDetails)
          .map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        IndividualTelephoneNumberForm.form
          .fill:
            request
              .individualProvidedDetails
              .telephoneNumber
      ))
