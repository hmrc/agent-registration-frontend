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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.UserProvidedNino
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNinoForm
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualNinoPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualNinoController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: IndividualNinoPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url)
    )
    .ensure(
      _.get[IndividualProvidedDetails].individualNino.fold(true) {
        case IndividualNino.FromAuth(_) => false
        case _ => true
      },
      implicit request =>
        logger.info(s"Nino is already provided from auth or citizen details. Skipping page and moving to next page.")
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(view(
          form = IndividualNinoForm.form
            .fill:
              request
                .get[IndividualProvidedDetails]
                .individualNino
                .map(_.toUserProvidedNino)
          ,
          linkId = linkId
        ))

  def submit(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId)
      .ensureValidFormAndRedirectIfSaveForLater[UserProvidedNino](
        IndividualNinoForm.form,
        implicit r => view(_, linkId)
      )
      .async:
        implicit request =>
          val validFormData: UserProvidedNino = request.get
          val updatedIndividualProvidedDetails: IndividualProvidedDetails = request.get[IndividualProvidedDetails]
            .modify(_.individualNino)
            .setTo(Some(validFormData))
          individualProvideDetailsService
            .upsert(updatedIndividualProvidedDetails)
            .map: _ =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
      .redirectIfSaveForLater

  extension (individualNino: IndividualNino)
    def toUserProvidedNino: UserProvidedNino =
      individualNino match
        case u: UserProvidedNino => u
        case h: IndividualNino.FromAuth => throw new IllegalArgumentException(s"Nino is already provided from auth enrolments (${h.nino})")
