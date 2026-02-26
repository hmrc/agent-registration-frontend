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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.NameMatchingForm
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.NameMatchingPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class NameMatchingController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  view: NameMatchingPage
)
extends FrontendController(mcc, actions):

  private type DataWithIndividualProvidedDetailsForSearch = List[IndividualProvidedDetails] *: AgentApplication *: DataWithAuth

  def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetailsForSearch] = actions
    .authorised
    .refine { implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) => request.add(agentApplication)
          case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    }.refine { implicit request =>
      individualProvideDetailsService
        .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
        .map: listOfIndividuals =>
          request.add[List[IndividualProvidedDetails]](listOfIndividuals)
    }

  def show(linkId: LinkId): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      Future.successful(Ok(view(
        form = NameMatchingForm.form,
        linkId = linkId
      )))

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      NameMatchingForm.form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(
            form = formWithErrors,
            linkId = linkId
          ))),
        providedName =>
          val agentProvidedNamesList = request.get[List[IndividualProvidedDetails]]
          if agentProvidedNamesList.isNamePresent(providedName) then
            Future.successful(Redirect(AppRoutes.providedetails.NameMatchConfrimationController.show))
          else
            Future.successful(Redirect(AppRoutes.providedetails.ContactApplicantController.show))
      )

extension (details: List[IndividualProvidedDetails])
  private def isNamePresent(name: IndividualName): Boolean = details
    .filter(_.internalUserId.isEmpty)
    .exists: agentProvidedDetails =>
      name === agentProvidedDetails.individualName
