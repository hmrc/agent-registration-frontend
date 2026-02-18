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
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualHmrcStandardForAgentsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualHmrcStandardForAgentsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  view: IndividualHmrcStandardForAgentsPage,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].individualSaUtr.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualSaUtrController.show(linkId))
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(view(linkId))

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .async:
      implicit request =>
        individualProvideDetailsService
          .upsert(
            request.get[IndividualProvidedDetails]
              .modify(_.hmrcStandardForAgentsAgreed)
              .setTo(StateOfAgreement.Agreed)
          ).map: _ =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId))
