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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedDateOfBirth
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualDateOfBirthForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualDateOfBirthPage

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualDateOfBirthController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualDateOfBirthPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)(using clock: Clock)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].emailAddress.isDefined,
      implicit request =>
        logger.info("Email address not yet provided. Redirecting to email page.")
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url)
    )
    .ensure(
      _.get[IndividualProvidedDetails].individualDateOfBirth.fold(true) {
        case IndividualDateOfBirth.FromCitizensDetails(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"Date of birth is already provided from citizens details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(view(
          IndividualDateOfBirthForm.form
            .fill:
              request.get[IndividualProvidedDetails]
                .individualDateOfBirth
                .map(_.toUserProvidedDateOfBirth)
          ,
          linkId = linkId
        ))

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .ensureValidForm[UserProvidedDateOfBirth](
      IndividualDateOfBirthForm.form,
      implicit r => view(_, linkId)
    )
    .async:
      implicit request =>
        val validFormData: UserProvidedDateOfBirth = request.get
        val updatedApplication: IndividualProvidedDetails = request.get[IndividualProvidedDetails]
          .modify(_.individualDateOfBirth)
          .setTo(Some(validFormData))
        individualProvideDetailsService
          .upsert(updatedApplication)
          .map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
