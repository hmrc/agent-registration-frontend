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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedSaUtr
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualSaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualSaUtrPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualSaUtrController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualSaUtrPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].individualNino.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualNinoController.show(linkId).url)
    )
    .ensure(
      _.get[IndividualProvidedDetails].individualSaUtr.fold(true) {
        case IndividualSaUtr.FromAuth(_) | IndividualSaUtr.FromCitizenDetails(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"SaUtr is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(view(
          form = IndividualSaUtrForm.form
            .fill:
              request.get[IndividualProvidedDetails]
                .individualSaUtr
                .map(_.toUserProvidedSaUtr)
          ,
          linkId = linkId
        ))

  def submit(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId)
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedSaUtr](
        IndividualSaUtrForm.form,
        implicit r => view(_, linkId)
      )
      .async:
        implicit request =>
          val validFormData: UserProvidedSaUtr = request.get
          val updatedIndividualProvidedDetails: IndividualProvidedDetails = request.get[IndividualProvidedDetails]
            .modify(_.individualSaUtr)
            .setTo(Some(validFormData))
          individualProvideDetailsService
            .upsert(updatedIndividualProvidedDetails)
            .map: _ =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
      .redirectIfSaveForLater
