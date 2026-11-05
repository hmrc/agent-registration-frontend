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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.DetailsAlreadyProvidedPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.ApplicationNotFoundPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.NameNotFoundPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.exitpages.NoIndividualRecordsLeftToClaimPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExitController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  agentApplicationService: AgentApplicationService,
  placeholderExitPage: SimplePage,
  detailsAlreadyProvidedPage: DetailsAlreadyProvidedPage,
  noRecordsLeftToClaimPage: NoIndividualRecordsLeftToClaimPage,
  applicationNotFoundPage: ApplicationNotFoundPage,
  nameNotFoundPage: NameNotFoundPage
)
extends FrontendController(mcc, actions):

  def detailsAlreadyProvided: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(detailsAlreadyProvidedPage())

  def genericExitPage: Action[AnyContent] = actions.action:
    implicit request =>
      Ok(placeholderExitPage(
        h1 = "You cannot use this service...",
        bodyText = Some(
          "Placeholder for member provided detailsgeneric exit page..."
        )
      ))

  def noRecordsLeftToClaim(linkId: LinkId): Action[AnyContent] = actions.authorised:
    implicit request =>
      Ok(noRecordsLeftToClaimPage(linkId))

  def applicationNotFound(linkId: LinkId): Action[AnyContent] =
    actions.action
      .ensure(
        condition =
          implicit request =>
            agentApplicationService
              .find(linkId)
              .map(_.isEmpty),
        resultWhenConditionNotMet =
          implicit request =>
            Redirect(AppRoutes.providedetails.StartController.start(linkId))
      ):
        implicit request =>
          Ok(applicationNotFoundPage(linkId))

  def nameNotFound(linkId: LinkId): Action[AnyContent] = actions.authorised:
    implicit request =>
      Ok(nameNotFoundPage(linkId))
