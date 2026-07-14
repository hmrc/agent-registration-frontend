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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.soletraderfailures.provideddetails

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedDateOfBirth
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix._10.IndividualDetailsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.soletraderfailures.provideddetails.IndividualDateOfBirthPage

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualDateOfBirthController @Inject() (
  actions: ApplicantActions,
  mcc: MessagesControllerComponents,
  view: IndividualDateOfBirthPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)(using clock: Clock)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions.getSoleTraderIdentityFix:
    implicit request =>
      Ok(view(
        IndividualDateOfBirthForm.form
          .fill:
            request.get[IndividualDetailsFix]
              .dateOfBirth
      ))

  def submit: Action[AnyContent] = actions.getSoleTraderIdentityFix
    .ensureValidForm[UserProvidedDateOfBirth](
      IndividualDateOfBirthForm.form,
      implicit r => view(_)
    )
    .async:
      implicit request =>
        val validFormData: UserProvidedDateOfBirth = request.get
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val fixableIndividual: RiskingOutcomeIndividual.FailedFixable =
          individualProvidedDetails.riskingOutcomeIndividual match
            case Some(fixable: RiskingOutcomeIndividual.FailedFixable) => fixable
            case _ =>
              throw new IllegalStateException(
                s"Expected a fixable risking outcome individual for Sole Trader with person ref ${individualProvidedDetails.personReference.value} but got ${individualProvidedDetails.riskingOutcomeIndividual}"
              )
        val updatedIndividualDetailsFix: IndividualDetailsFix = request.get[IndividualDetailsFix]
          .modify(_.dateOfBirth)
          .setTo(Some(validFormData))
        val updatedFixes: Seq[IndividualFix] = fixableIndividual.fixes
          .map:
            case _: IndividualDetailsFix => updatedIndividualDetailsFix
            case otherFix => otherFix
        val updatedRiskingOutcomeIndividual: RiskingOutcomeIndividual.FailedFixable = fixableIndividual
          .modify(_.fixes)
          .setTo(updatedFixes)
        individualProvideDetailsService
          .upsert(
            request.get[IndividualProvidedDetails]
              .modify(_.riskingOutcomeIndividual)
              .setTo(Some(updatedRiskingOutcomeIndividual))
          )
          .map: _ =>
            Redirect(AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.show.url)
